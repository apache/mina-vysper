/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xmpp.extension.xep0124;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the session state of a BOSH client
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshBackedSessionContext extends AbstractSessionContext implements StanzaWriter {

    private final static Logger LOGGER = LoggerFactory.getLogger(BoshBackedSessionContext.class);

    private final BoshHandler boshHandler;
    
    private final int maxpause = 120;

    private final int inactivity = 60;

    private final int polling = 15;
    
    private final int maximumSentResponses = 10;
    
    /*
     * The number of milliseconds that will have to pass for a response to be reported missing to the client by
     * responding with a report and time attributes. See Response Acknowledgements in XEP-0124.
     */
    private final int brokenConnectionReportTimeout = 1000;
    
    /*
     * Keeps the suspended HTTP requests (does not respond to them) until the server has an asynchronous message
     * to send to the client. (Comet HTTP Long Polling technique - described in XEP-0124)
     * 
     * The BOSH requests are sorted by their RIDs.
     */
    private final SortedMap<Long, BoshRequest> requestsWindow = new TreeMap<Long, BoshRequest>();

    /*
     * Keeps the asynchronous messages sent from server that cannot be delivered to the client because there are
     * no available HTTP requests to respond to (requestsWindow is empty).
     */
    private final Queue<Stanza> delayedResponseQueue = new LinkedList<Stanza>();
    
    /*
     * A cache of sent responses to the BOSH client, kept in the event of delivery failure and retransmission requests.
     * See Broken Connections in XEP-0124.
     */
    private final SortedMap<Long, BoshResponse> sentResponses = new TreeMap<Long, BoshResponse>();

    private int requests = 2;

    private String boshVersion = "1.9";

    private String contentType = BoshServlet.XML_CONTENT_TYPE;

    private int wait = 60;

    private int hold = 1;
    
    private int currentInactivity = inactivity;
    
    /*
     * The highest RID that can be read and processed, this is the highest (rightmost) contiguous RID.
     * The requests from the client can come theoretically with missing updates:
     * rid_1, rid_2, rid_4 (missing rid_3, highestReadRid is rid_2)
     * 
     * must be synchronized along with requestsWindow 
     */
    private Long highestReadRid = null;

    /**
     * 
     * must be synchronized along with requestsWindow 
     */
    private Long currentProcessingRequest = null;

    /**
     * must be synchronized along with requestsWindow 
     */
    private BoshRequest latestEmptyPollingRequest = null;
    
    /*
     * Indicate if the BOSH client will use acknowledgements throughout the session and that the absence of an 'ack'
     * attribute in any request is meaningful.
     */
    private boolean clientAcknowledgements;
    
    /*
     * The timestamp of the latest response wrote to the client is used to measure the inactivity period.
     * When reaching the maximum inactivity the session will automatically close.
     */
    private long latestWriteTimestamp = System.currentTimeMillis();
    
    private final InactivityChecker inactivityChecker;
    
    private Long lastInactivityExpireTime;
    
    private boolean isWatchedByInactivityChecker;

    /**
     * Creates a new context for a session
     * @param boshHandler
     * @param serverRuntimeContext
     * @param inactivityChecker
     */
    public BoshBackedSessionContext(BoshHandler boshHandler, ServerRuntimeContext serverRuntimeContext, InactivityChecker inactivityChecker) {
        super(serverRuntimeContext, new SessionStateHolder());

        // in BOSH we jump directly to the encrypted state
        sessionStateHolder.setState(SessionState.ENCRYPTED);

        this.boshHandler = boshHandler;

        this.inactivityChecker = inactivityChecker;
        updateInactivityChecker();
    }
    
    public boolean isWatchedByInactivityChecker() {
        return isWatchedByInactivityChecker;
    }
    
    private synchronized void updateInactivityChecker() {
        Long newInactivityExpireTime = null;
        if (requestsWindow.isEmpty()) {
            newInactivityExpireTime = latestWriteTimestamp + currentInactivity * 1000;
            if (newInactivityExpireTime.equals(lastInactivityExpireTime)) {
                return;
            }
        } else if (!isWatchedByInactivityChecker) {
            return;
        }
        isWatchedByInactivityChecker = inactivityChecker.updateExpireTime(this, lastInactivityExpireTime, newInactivityExpireTime);
        lastInactivityExpireTime = newInactivityExpireTime;
    }
    
    /**
     * Returns the highest RID that is received in a continuous (uninterrupted) sequence of RIDs.
     * Higher RIDs can exist with gaps separating them from the highestReadRid.
     * @return the highest continuous RID received so far
     */
    public long getHighestReadRid() {
        return highestReadRid;
    }

    public SessionStateHolder getStateHolder() {
        return sessionStateHolder;
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void setIsReopeningXMLStream() {
        // BOSH does not use XML streams, the BOSH equivalent for reopening an XML stream is to restart the BOSH connection,
        // and this is done in BoshHandler when the client requests it
    }

    /*
     * This method is synchronized on the session object to prevent concurrent writes to the same BOSH client
     */
    synchronized public void write(Stanza stanza) {
        if (stanza == null) throw new IllegalArgumentException("stanza must not be null.");
        LOGGER.debug("adding server stanza for writing to BOSH client");
        writeBoshResponse(BoshStanzaUtils.wrapStanza(stanza));
    }

    /**
     * Writes a BOSH response (that is wrapped in a &lt;body/&gt; element) if there are available HTTP requests
     * to respond to, otherwise the response is queued to be sent later (when a HTTP request will be available).
     * <p>
     * (package access)
     * 
     * @param responseStanza The BOSH response to write
     */
    /*package*/ void writeBoshResponse(Stanza responseStanza) {
        if (responseStanza == null) throw new IllegalArgumentException();
        final boolean isEmtpyResponse = responseStanza == BoshStanzaUtils.EMPTY_BOSH_RESPONSE;
        
        BoshRequest req;
        BoshResponse boshResponse;
        synchronized (requestsWindow) {
            if (requestsWindow.isEmpty() || requestsWindow.firstKey() > highestReadRid) {
                if (isEmtpyResponse) return; // do not delay empty responses
                final boolean accepted = delayedResponseQueue.offer(responseStanza);
                if (!accepted) {
                    LOGGER.debug("rid = {} - request not queued. BOSH delayedResponseQueue is full: {}", 
                            requestsWindow.firstKey(), delayedResponseQueue.size());
                    // TODO do not silently drop this stanza
                }
                return;
            }
            req = requestsWindow.remove(requestsWindow.firstKey());
            boshResponse = getBoshResponse(responseStanza, req.getRid().equals(highestReadRid) ? null : highestReadRid);
            if (LOGGER.isDebugEnabled()) {
                String emptyHint = isEmtpyResponse ? "empty " : StringUtils.EMPTY;
                LOGGER.debug("rid = " + req.getRid() + " - BOSH writing {}response: {}", emptyHint, new String(boshResponse.getContent()));
            }
        }

        if (isResponseSavable(req, responseStanza)) {
            synchronized (sentResponses) {
                sentResponses.put(req.getRid(), boshResponse);
                // The number of responses to non-pause requests kept in the buffer SHOULD be either the same as the maximum
                // number of simultaneous requests allowed or, if Acknowledgements are being used, the number of responses
                // that have not yet been acknowledged (this part is handled in insertRequest(BoshRequest)), or 
                // the hard limit maximumSentResponses (not in the specification) that prevents excessive memory consumption.
                if (sentResponses.size() > maximumSentResponses || (!isClientAcknowledgements() && sentResponses.size() > requests)) {
                    sentResponses.remove(sentResponses.firstKey());
                }
            }
        }

        final AsyncContext asyncContext = this.saveResponse(req, boshResponse);
        asyncContext.dispatch();
        latestWriteTimestamp = System.currentTimeMillis();
        updateInactivityChecker();
    }
    
    private static boolean isResponseSavable(BoshRequest req, Stanza response) {
        // responses to pause requests are not saved
        if (req.getBody().getAttributeValue("pause") != null) {
            return false;
        }
        // responses with binding error are not saved
        for (XMLElement element : response.getInnerElements()) {
            if ("iq".equals(element.getName()) && "error".equals(element.getAttributeValue("type"))) {
                for (XMLElement subelement : element.getInnerElements()) {
                    if ("bind".equals(subelement.getName())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Writes an error to the client and closes the connection
     * @param br 
     * @param condition the error condition
     */
    private void error(BoshRequest br, String condition) {
        final Long rid = br.getRid();
        requestsWindow.put(rid, br);
        BoshRequest req = requestsWindow.remove(requestsWindow.firstKey());
        Stanza body = BoshStanzaUtils.createTerminateResponse(condition).build();
        BoshResponse boshResponse = getBoshResponse(body, null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rid = {} - BOSH writing response: {}", rid, new String(boshResponse.getContent()));
        }

        final AsyncContext asyncContext = this.saveResponse(req, boshResponse);
        asyncContext.dispatch();
        close();
    }

    /*
     * Terminates the BOSH session
     */
    synchronized public void close() {
        // respond to all the queued HTTP requests with termination responses
        while (!requestsWindow.isEmpty()) {
            BoshRequest req = requestsWindow.remove(requestsWindow.firstKey());
            Stanza body = BoshStanzaUtils.TERMINATE_BOSH_RESPONSE;
            BoshResponse boshResponse = getBoshResponse(body, null);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("rid = {} - BOSH writing response: {}", req.getRid(), new String(boshResponse.getContent()));
            }

            final AsyncContext asyncContext =
                    this.saveResponse(req, boshResponse);
            asyncContext.dispatch();
        }
        
        serverRuntimeContext.getResourceRegistry().unbindSession(this);
        sessionStateHolder.setState(SessionState.CLOSED);
        
        inactivityChecker.updateExpireTime(this, lastInactivityExpireTime, null);
        lastInactivityExpireTime = null;
        
        LOGGER.info("BOSH session {} closed", getSessionId());
    }

    public void switchToTLS(boolean delayed, boolean clientTls) {
        // BOSH cannot switch dynamically (because STARTTLS cannot be used with HTTP),
        // SSL can be enabled/disabled in BoshEndpoint#setSSLEnabled()
    }

    /**
     * Setter for the Content-Type header of the HTTP responses sent to the BOSH client associated with this session
     * @param contentType
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Getter for the Content-Type header
     * @return the configured Content-Type
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * Getter for the maximum length of a temporary session pause (in seconds) that a client can request
     * @return
     */
    public int getMaxPause() {
        return maxpause;
    }

    /**
     * Setter for the BOSH 'wait' parameter, the longest time (in seconds) that the connection manager is allowed to
     * wait before responding to any request during the session. The BOSH client can only configure this parameter to
     * a lower value than the default value from this session context.
     * @param wait the BOSH 'wait' parameter
     */
    public void setWait(int wait) {
        this.wait = Math.min(wait, this.wait);
    }

    /**
     * Getter for the BOSH 'wait' parameter
     * @return The BOSH 'wait' parameter
     */
    public int getWait() {
        return wait;
    }

    /**
     * Setter for the BOSH 'hold' parameter, the maximum number of HTTP requests the connection manager is allowed to
     * keep waiting at any one time during the session. The value of this parameter can trigger the modification of
     * the BOSH 'requests' parameter.
     * @param hold
     */
    public void setHold(int hold) {
        this.hold = hold;
        if (hold >= 2) {
            requests = hold + 1;
        }
    }

    /**
     * Getter for the BOSH 'hold' parameter
     * @return the BOSH 'hold' parameter
     */
    public int getHold() {
        return hold;
    }
    
    /**
     * Setter for the client acknowledgements throughout the session
     * @param value true is enabled, false otherwise
     */
    public void setClientAcknowledgements(boolean value) {
        clientAcknowledgements = value;
    }
    
    /**
     * Getter for client acknowledgements
     * @return true if enabled, false otherwise
     */
    public boolean isClientAcknowledgements() {
        return clientAcknowledgements;
    }

    /**
     * Setter for the highest version of the BOSH protocol that the connection manager supports, or the version
     * specified by the client in its request, whichever is lower.
     * @param version the BOSH version
     */
    public void setBoshVersion(String version) throws NumberFormatException {
        try {
            String[] v = boshVersion.split("\\.");
            int major = Integer.parseInt(v[0]);
            int minor = Integer.parseInt(v[1]);
            v = version.split("\\.");

            if (v.length == 2) {
                int clientMajor = Integer.parseInt(v[0]);
                int clientMinor = Integer.parseInt(v[1]);
    
                if (clientMajor < major || (clientMajor == major && clientMinor < minor)) {
                    boshVersion = version;
                }
            }
        } catch (NumberFormatException e) {
            throw e;
        }
    }

    /**
     * Getter for the BOSH protocol version
     * @return the BOSH version
     */
    public String getBoshVersion() {
        return boshVersion;
    }

    /**
     * Getter for the BOSH 'inactivity' parameter, the longest allowable inactivity period (in seconds).
     * @return the BOSH 'inactivity' parameter
     */
    public int getInactivity() {
        return inactivity;
    }

    /**
     * Getter for the BOSH 'polling' parameter, the shortest allowable polling interval (in seconds).
     * @return the BOSH 'polling' parameter
     */
    public int getPolling() {
        return polling;
    }

    /**
     * Getter for the BOSH 'requests' parameter, the limit number of simultaneous requests the client makes.
     * @return the BOSH 'requests' parameter
     */
    public int getRequests() {
        return requests;
    }

    /*
     * A request expires when it stays enqueued in the requestsWindow longer than the allowed 'wait' time.
     * The synchronization on the session object ensures that there will be no concurrent writes or other concurrent
     * expirations for the BOSH client while the current request expires.
     */
    private synchronized void requestExpired(final AsyncContext context) {
        final BoshRequest req =
                (BoshRequest) context.getRequest().getAttribute("request");
        if (req == null) {
            LOGGER.warn("Continuation expired without having "
                    + "an associated request!");
            return;
        }
        LOGGER.debug("rid = {} - BOSH request expired", req.getRid());
        while (!requestsWindow.isEmpty() && requestsWindow.firstKey() <= req.getRid()) {
            writeBoshResponse(BoshStanzaUtils.EMPTY_BOSH_RESPONSE);
        }
    }

    /**
     * Suspends and enqueues an HTTP request to be used later when an asynchronous message needs to be sent from
     * the connection manager to the BOSH client.
     * 
     * @param req the HTTP request
     */
    public void insertRequest(final BoshRequest br) {

        final Stanza stanza = br.getBody();
        final Long rid = br.getRid();
        LOGGER.debug("rid = {} - inserting new BOSH request", rid);
        
        // reset the inactivity
        currentInactivity = inactivity;
        
        final AsyncContext context = br.getHttpServletRequest().startAsync();
        this.addContinuationExpirationListener(context);
        context.setTimeout(this.wait * 1000);
        br.getHttpServletRequest().setAttribute("request", br);

        final int currentRequests = requests;
        synchronized (requestsWindow) {
            if (highestReadRid != null && highestReadRid + currentRequests < rid) {
                LOGGER.warn("rid = {} - received RID >= the permitted window of concurrent requests ({})",
                        rid, highestReadRid);
                error(br, "item-not-found");
                return;
            }
            if (highestReadRid != null && rid <= highestReadRid) {
                synchronized (sentResponses) {
                    if (sentResponses.containsKey(rid)) {
                        // Resending the old response
                        resendResponse(br);
                    } else {
                        LOGGER.warn("rid = {} - BOSH response not in buffer error");
                        error(br, "item-not-found");
                    }
                }
                return;
            }
            if (requestsWindow.size() + 1 > currentRequests && !"terminate".equals(stanza.getAttributeValue("type"))
                    && stanza.getAttributeValue("pause") == null) {
                LOGGER.warn("BOSH Overactivity: Too many simultaneous requests");
                error(br, "policy-violation");
                return;
            }
            if (requestsWindow.size() + 1 == currentRequests && !"terminate".equals(stanza.getAttributeValue("type"))
                    && stanza.getAttributeValue("pause") == null && stanza.getInnerElements().isEmpty()) {
                if (!requestsWindow.isEmpty()
                        && br.getTimestamp() - requestsWindow.get(requestsWindow.lastKey()).getTimestamp() < polling * 1000) {
                    LOGGER.warn("BOSH Overactivity: Too frequent requests");
                    error(br, "policy-violation");
                    return;
                }
            }
            if ((wait == 0 || hold == 0) && stanza.getInnerElements().isEmpty()) {
                if (latestEmptyPollingRequest != null && br.getTimestamp() - latestEmptyPollingRequest.getTimestamp() < polling * 1000) {
                    LOGGER.warn("BOSH Overactivity for polling: Too frequent requests");
                    error(br, "policy-violation");
                    return;
                }
                latestEmptyPollingRequest = br;
            }

            requestsWindow.put(rid, br);
            updateInactivityChecker();

            if (highestReadRid == null) {
                highestReadRid = rid;
            }
            for (;;) {
                // update the highestReadRid to the latest value
                // it is possible to have higher RIDs than the highestReadRid with a gap between them (e.g. lost client request)
                if (requestsWindow.containsKey(highestReadRid + 1)) {
                    highestReadRid++;
                } else {
                    break;
                }
            }
        }


        if (isClientAcknowledgements()) {
            synchronized (sentResponses) {
                if (stanza.getAttribute("ack") == null) {
                    // if there is no ack attribute present then the client confirmed it received all the responses to all the previous requests
                    // and we clear the cache
                    sentResponses.clear();
                } else if (!sentResponses.isEmpty()) {
                    // After receiving a request with an 'ack' value less than the 'rid' of the last request that it has already responded to,
                    // the connection manager MAY inform the client of the situation. In this case it SHOULD include a 'report' attribute set
                    // to one greater than the 'ack' attribute it received from the client, and a 'time' attribute set to the number of milliseconds
                    // since it sent the response associated with the 'report' attribute.
                    long ack = Long.parseLong(stanza.getAttributeValue("ack"));
                    if (ack < sentResponses.lastKey() && sentResponses.containsKey(ack + 1)) {
                        long delta = System.currentTimeMillis() - sentResponses.get(ack + 1).getTimestamp();
                        if (delta >= brokenConnectionReportTimeout) {
                            sendBrokenConnectionReport(ack + 1, delta);
                            return;
                        }
                    }
                }
            }
        }
        
        // we cannot pause if there are missing requests, this is tested with
        // br.getRid().equals(requestsWindow.lastKey()) && highestReadRid.equals(br.getRid())
        synchronized (requestsWindow) {
            final String pauseAttribute = stanza.getAttributeValue("pause");
            if (pauseAttribute != null && rid.equals(requestsWindow.lastKey()) && highestReadRid.equals(rid)) {
                int pause;
                try {
                    pause = Integer.parseInt(pauseAttribute);
                } catch (NumberFormatException e) {
                    error(br, "bad-request");
                    return;
                }
                pause = Math.max(0, pause);
                pause = Math.min(pause, maxpause);
                respondToPause(pause);
                return;
            }
        }

        // If there are delayed responses waiting to be sent to the BOSH client, then we wrap them all in
        // a <body/> element and send them as a HTTP response to the current HTTP request.
        Stanza delayedResponse;
        ArrayList<Stanza> mergeCandidates = null; // do not create until there is a delayed response
        while ((delayedResponse = delayedResponseQueue.poll()) != null) {
            if (mergeCandidates == null) mergeCandidates = new ArrayList<Stanza>();
            mergeCandidates.add(delayedResponse);
        }
        Stanza mergedResponse = BoshStanzaUtils.mergeResponses(mergeCandidates);
        if (mergedResponse != null) {
            writeBoshResponse(mergedResponse);
            return;
        }

        // If there are more suspended enqueued requests than it is allowed by the BOSH 'hold' parameter,
        // than we release the oldest one by sending an empty response.
        if (requestsWindow.size() > hold) {
            writeBoshResponse(BoshStanzaUtils.EMPTY_BOSH_RESPONSE);
        }
    }
    
    protected void respondToPause(int pause) {
        LOGGER.debug("Setting inactivity period to {}", pause);
        currentInactivity = pause;
        for (;;) {
            BoshRequest boshRequest = getNextRequest();
            if (boshRequest == null) {
                break;
            }
            writeBoshResponse(BoshStanzaUtils.EMPTY_BOSH_RESPONSE);
        }
    }
    
    protected void sendBrokenConnectionReport(long report, long delta) {
        StanzaBuilder stanzaBuilder = BoshStanzaUtils.createBrokenSessionReport(report, delta);
        writeBoshResponse(stanzaBuilder.build());
    }
    
    protected void addContinuationExpirationListener(final AsyncContext context) {
        // listen the continuation to be notified when the request expires
        context.addListener(new AsyncListener() {

            public void onTimeout(AsyncEvent event) throws IOException {
                BoshBackedSessionContext.this.requestExpired(context);
            }

            public void onStartAsync(AsyncEvent event) throws IOException {
                // ignore
            }

            public void onError(AsyncEvent event) throws IOException {
                LOGGER.warn("Async error", event.getThrowable());
            }

            public void onComplete(AsyncEvent event) throws IOException {
                // ignore
            }
        });
    }

    protected void resendResponse(BoshRequest br) {
        final Long rid = br.getRid();
        BoshResponse boshResponse = sentResponses.get(rid);
        if (boshResponse == null) {
            LOGGER.debug("rid = {} - BOSH response could not (no longer) be retrieved for resending.", rid);
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rid = {} - BOSH writing response (resending): {}", rid, new String(boshResponse.getContent()));
        }

        final AsyncContext asyncContext = this.saveResponse(br, boshResponse);
        asyncContext.dispatch();
        this.latestWriteTimestamp = System.currentTimeMillis();
        this.updateInactivityChecker();
    }

    protected BoshResponse getBoshResponse(Stanza stanza, Long ack) {
        if (ack != null) {
            stanza = BoshStanzaUtils.addAttribute(stanza, "ack", ack.toString());
        }
        byte[] content = new Renderer(stanza).getComplete().getBytes();
        return new BoshResponse(contentType, content);
    }

    /**
     * Returns the next BOSH body to process.
     * It is possible to have more than one BOSH body to process in the case where a lost request is resent by the client.
     * @return the next (by RID order) body to process
     */
    public BoshRequest getNextRequest() {
        synchronized (requestsWindow) {
            if (requestsWindow.isEmpty()) {
                return null;
            }
            if (currentProcessingRequest == null || currentProcessingRequest < requestsWindow.firstKey()) {
                currentProcessingRequest = requestsWindow.firstKey();
            }
            if (currentProcessingRequest > highestReadRid) {
                return null;
            } else {
                currentProcessingRequest++;
                return requestsWindow.get(currentProcessingRequest - 1);
            }
        }
    }

    protected AsyncContext saveResponse(final BoshRequest boshRequest, final BoshResponse boshResponse) {
        final HttpServletRequest request = boshRequest.getHttpServletRequest();
        final AsyncContext asyncContext = request.getAsyncContext();
        request.setAttribute("response", boshResponse);
        return asyncContext;
    }
}
