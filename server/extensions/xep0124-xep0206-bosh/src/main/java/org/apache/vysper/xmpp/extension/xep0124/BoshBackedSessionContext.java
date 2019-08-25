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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
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

    public final static String HTTP_SESSION_ATTRIBUTE = "org.apache.vysper.xmpp.extension.xep0124.BoshBackedSessionContext";
    
    public final static String BOSH_REQUEST_ATTRIBUTE = "boshRequest";
    public final static String BOSH_RESPONSE_ATTRIBUTE = "boshResponse";

    protected static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    
    private final int maxpauseSeconds = 120;

    private final int inactivitySeconds = 60;

    private final int pollingSeconds = 15;
    
    private final int maximumSentResponses = 100;
        
    /*
     * The number of milliseconds that will have to pass for a response to be reported missing to the client by
     * responding with a report and time attributes. See Response Acknowledgements in XEP-0124.
     */
    private final int brokenConnectionReportTimeoutMillis = 1000;
    
    /*
     * Keeps the suspended HTTP requests (does not respond to them) until the server has an asynchronous message
     * to send to the client. (Comet HTTP Long Polling technique - described in XEP-0124)
     * 
     * The BOSH requests are sorted by their RIDs.
     */
    protected final RequestsWindow requestsWindow;

    /*
     * Keeps the asynchronous messages sent from server that cannot be delivered to the client because there are
     * no available HTTP requests to respond to (requestsWindow is empty).
     */
    private final Queue<Stanza> delayedResponseQueue = new LinkedList<Stanza>();
    
    /*
     * A cache of sent responses to the BOSH client, kept in the event of delivery failure and retransmission requests.
     * these sent responses are moved to the sentResponsesBacklog when the client acks their receival.
     * See Broken Connections in XEP-0124.
     */
    private final SortedMap<Long, BoshResponse> sentResponses = new TreeMap<Long, BoshResponse>();

    /**
     * backlog of sent responses which have been acked by the client (and thus shouldn't ever been needed to be resent.
     */
    private final ResponsesBuffer sentResponsesBacklog = new ResponsesBuffer();
    
    private int parallelRequestsCount = 2;

    private String boshVersion = "1.9";

    private String contentType = BoshServlet.XML_CONTENT_TYPE;

    private int wait = 60;

    private int hold = 1;
    
    private int currentInactivitySeconds = inactivitySeconds;
    
    /**
     * must be synchronized along with requestsWindow 
     */
    private Long latestEmptyPollingRequestTimestamp = 0L;
    
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
    
    private boolean propagateSessionContextToHTTPSession = false;

    /**
     * Creates a new context for a session
     * @param serverRuntimeContext
     * @param stanzaProcessor
     * @param inactivityChecker
     */
    public BoshBackedSessionContext(ServerRuntimeContext serverRuntimeContext, StanzaProcessor stanzaProcessor, BoshHandler boshHandler, InactivityChecker inactivityChecker) {
        super(serverRuntimeContext, stanzaProcessor, new SessionStateHolder());

        // in BOSH we jump directly to the encrypted state
        sessionStateHolder.setState(SessionState.ENCRYPTED);

        requestsWindow = new RequestsWindow(getSessionId());
        this.inactivityChecker = inactivityChecker;
        updateInactivityChecker();
    }

    public boolean isWatchedByInactivityChecker() {
        return isWatchedByInactivityChecker;
    }
    
    private synchronized void updateInactivityChecker() {
        Long newInactivityExpireTime = null;
        if (requestsWindow.isEmpty()) {
            newInactivityExpireTime = latestWriteTimestamp + currentInactivitySeconds * 1000;
            if (newInactivityExpireTime.equals(lastInactivityExpireTime)) {
                return;
            }
        } else if (!isWatchedByInactivityChecker) {
            return;
        }
        isWatchedByInactivityChecker = inactivityChecker.updateExpireTime(this, lastInactivityExpireTime, newInactivityExpireTime);
        lastInactivityExpireTime = newInactivityExpireTime;
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

    /**
     * true, iff this session context will be stored to the related BOSH HTTP session.
     * @return
     */
    public boolean propagateSessionContext() {
        return propagateSessionContextToHTTPSession;
    }

    /*
    * This method is synchronized on the session object to prevent concurrent writes to the same BOSH client
    */
    synchronized public void write(Stanza stanza) {
        if (stanza == null) throw new IllegalArgumentException("stanza must not be null.");
        LOGGER.debug("SID = " + getSessionId() + " - adding server stanza for writing to BOSH client");
        writeBoshResponse(BoshStanzaUtils.wrapStanza(stanza));
    }

    /**
     * Writes a server-to-client XMPP stanza as a BOSH response (wrapped in a &lt;body/&gt; element) if there are 
     * available HTTP requests to respond to, otherwise the response is queued to be sent later 
     * (when a HTTP request becomes available).
     * <p>
     * (package access)
     * 
     * @param responseStanza The BOSH response to write
     */
    /*package*/ void writeBoshResponse(Stanza responseStanza) {
        if (responseStanza == null) throw new IllegalArgumentException();
        final boolean isEmtpyResponse = responseStanza == BoshStanzaUtils.EMPTY_BOSH_RESPONSE;
        
        final ArrayList<BoshRequest> boshRequestsForRID = new ArrayList<BoshRequest>(1);
        BoshResponse boshResponse;
        final Long rid;
        synchronized (requestsWindow) {
            BoshRequest req = requestsWindow.pollNext();
            if (req == null) {
                if (isEmtpyResponse) return; // do not delay empty responses, everything's good.
                // delay sending until request comes available
                final boolean accepted = delayedResponseQueue.offer(responseStanza);
                if (!accepted) {
                    LOGGER.debug("SID = " + getSessionId() + " - stanza not queued. BOSH delayedResponseQueue is full: {}", 
                            delayedResponseQueue.size());
                    // TODO do not silently drop this stanza
                }
                return;            
            }

            rid = req.getRid();
            // in rare cases, we have same RID in two separate requests
            boshRequestsForRID.add(req);
            
            // collect more requests for this RID
            while (rid.equals(requestsWindow.firstRid())) {
                final BoshRequest sameRidRequest = requestsWindow.pollNext();
                boshRequestsForRID.add(sameRidRequest);
                LOGGER.warn("SID = " + getSessionId() + " - rid = {} - multi requests ({}) per RID.", rid, boshRequestsForRID.size());
            }
            
            long highestContinuousRid = requestsWindow.getHighestContinuousRid();
            final Long ack = rid.equals(highestContinuousRid) ? null : highestContinuousRid;
            boshResponse = getBoshResponse(responseStanza, ack);
            if (LOGGER.isDebugEnabled()) {
                String emptyHint = isEmtpyResponse ? "empty " : StringUtils.EMPTY;
                LOGGER.debug("SID = " + getSessionId() + " - rid = " + rid + " - BOSH writing {}response: {}", emptyHint, new String(boshResponse.getContent()));
            }
        }

        synchronized (sentResponses) {
            if (isResponseSavable(boshRequestsForRID.get(0), responseStanza)) {
                sentResponses.put(rid, boshResponse);
                // The number of responses to non-pause requests kept in the buffer SHOULD be either the same as the maximum
                // number of simultaneous requests allowed or, if Acknowledgements are being used, the number of responses
                // that have not yet been acknowledged (this part is handled in insertRequest(BoshRequest)), or 
                // the hard limit maximumSentResponses (not in the specification) that prevents excessive memory consumption.
                if (sentResponses.size() > maximumSentResponses || (!isClientAcknowledgements() && sentResponses.size() > parallelRequestsCount)) {
                    final Long key = sentResponses.firstKey();
                    sentResponsesBacklog.add(key, sentResponses.remove(key));
                }
            }
        }
        
        if (sentResponses.size() > maximumSentResponses + 10) {
            synchronized (sentResponses) {
                LOGGER.warn("stored sent responses ({}) exeeds maximum ({}). purging.", sentResponses.size(), maximumSentResponses);
                while (sentResponses.size() > maximumSentResponses) {
                    final Long key = sentResponses.firstKey();
                    sentResponsesBacklog.add(key, sentResponses.remove(key));
                }
            }
        }

        for (BoshRequest boshRequest : boshRequestsForRID) {
            try {
                final AsyncContext asyncContext = saveResponse(boshRequest, boshResponse);
                asyncContext.dispatch();
            } catch (Exception e) {
                LOGGER.warn("SID = " + getSessionId() + " - exception in async processing rid = {}", boshRequest.getRid(), e);
            }
        }
        setLatestWriteTimestamp();
        updateInactivityChecker();
    }

    private String logRIDSequence() {
        final String logMsg = requestsWindow.logRequestWindow();
        return logMsg + ", sent buffer = " + sentResponses.size();
    }

    private void setLatestWriteTimestamp() {
        latestWriteTimestamp = System.currentTimeMillis();
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
    
    public void sendError(String condition) {
        sendError(null, condition);
    }

    /**
     * Writes an error to the client and closes the connection
     * @param condition the error condition
     */
    protected void sendError(BoshRequest req, String condition) {
        req = req == null ? requestsWindow.pollNext() : req;
        if (req == null) {
            LOGGER.warn("SID = " + getSessionId() + " - no request for sending BOSH error " + condition);
            endSession(SessionTerminationCause.CONNECTION_ABORT);
            return;
        }
        final Long rid = req.getRid();
        Stanza body = BoshStanzaUtils.createTerminateResponse(condition).build();
        BoshResponse boshResponse = getBoshResponse(body, null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SID = " + getSessionId() + " - rid = {} - BOSH writing response: {}", rid, new String(boshResponse.getContent()));
        }

        try {
            final AsyncContext asyncContext = saveResponse(req, boshResponse);
            asyncContext.dispatch();
        } catch (Exception e) {
            LOGGER.warn("SID = " + getSessionId() + " - exception in async processing", e);
        }
        close();
    }

    /*
     * Terminates the BOSH session
     */
    public void close() {
        // respond to all the queued HTTP requests with termination responses
        synchronized (requestsWindow) {
            BoshRequest next;
            while ((next = requestsWindow.pollNext()) != null) {
                Stanza body = BoshStanzaUtils.TERMINATE_BOSH_RESPONSE;
                BoshResponse boshResponse = getBoshResponse(body, null);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("SID = " + getSessionId() + " - rid = {} - BOSH writing response: {}", next.getRid(), new String(boshResponse.getContent()));
                }
    
                try {
                    final AsyncContext asyncContext = saveResponse(next, boshResponse);
                    asyncContext.dispatch();
                } catch (Exception e) {
                    LOGGER.warn("SID = " + getSessionId() + " - exception in async processing", e);
                }
            }

            inactivityChecker.updateExpireTime(this, lastInactivityExpireTime, null);
            lastInactivityExpireTime = null;
        }

        LOGGER.info("SID = " + getSessionId() + " - session closed");
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
        return maxpauseSeconds;
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
            parallelRequestsCount = hold + 1;
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
        return inactivitySeconds;
    }

    /**
     * Getter for the BOSH 'polling' parameter, the shortest allowable polling interval (in seconds).
     * @return the BOSH 'polling' parameter
     */
    public int getPolling() {
        return pollingSeconds;
    }

    /**
     * Getter for the BOSH 'requests' parameter, the limit number of simultaneous requests the client makes.
     * @return the BOSH 'requests' parameter
     */
    public int getRequests() {
        return parallelRequestsCount;
    }

    /*
     * A request expires when it stays enqueued in the requestsWindow longer than the allowed 'wait' time.
     * The synchronization on the session object ensures that there will be no concurrent writes or other concurrent
     * expirations for the BOSH client while the current request expires.
     */
    private void requestExpired(final AsyncContext context) {
        final BoshRequest req =
                (BoshRequest) context.getRequest().getAttribute(BOSH_REQUEST_ATTRIBUTE);
        if (req == null) {
            LOGGER.warn("SID = " + getSessionId() + " - Continuation expired without having "
                    + "an associated request!");
            return;
        }
        LOGGER.debug("SID = " + getSessionId() + " - rid = {} - BOSH request expired", req.getRid());
        while (!requestsWindow.isEmpty() && requestsWindow.firstRid() <= req.getRid()) {
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

        final Stanza boshOuterBody = br.getBody();
        final Long rid = br.getRid();
        LOGGER.debug("SID = " + getSessionId() + " - rid = {} - inserting new BOSH request", rid);
        
        // reset the inactivity
        currentInactivitySeconds = inactivitySeconds;

        final HttpServletRequest request = br.getHttpServletRequest();
        request.setAttribute(BOSH_REQUEST_ATTRIBUTE, br);
        final AsyncContext context = request.startAsync();
        addContinuationExpirationListener(context);
        context.setTimeout(this.wait * 1000);

        // allow two more parallel request, be generous in what you receive
        final int maxToleratedParallelRequests = parallelRequestsCount + 2;
        synchronized (requestsWindow) {

            // only allow 'parallelRequestsCount' request to be queued
            final long highestContinuousRid = requestsWindow.getHighestContinuousRid();
            if (highestContinuousRid != -1 && rid > highestContinuousRid + maxToleratedParallelRequests) {
                LOGGER.warn("SID = " + getSessionId() + " - rid = {} - received RID >= the permitted window of concurrent requests ({})",
                        rid, highestContinuousRid);
                // don't queue // queueRequest(br);
                sendError(br, "item-not-found");
                return;
            }
            
            // resend missed responses
            final boolean resend = rid <= requestsWindow.getCurrentProcessingRequest();
            if (resend) {
            // OLD: if (highestContinuousRid != null && rid <= highestContinuousRid) {                
                synchronized (sentResponses) {
                    if (LOGGER.isInfoEnabled()) {
                        final String pendingRids = requestsWindow.logRequestWindow();
                        final String sentRids = logSentResponsesBuffer();
                        LOGGER.info("SID = " + getSessionId() + " - rid = {} - resend request. sent buffer: {} - req.win.: " + pendingRids, rid, sentRids);
                    }
                    if (sentResponses.containsKey(rid)) {
                        LOGGER.info("SID = " + getSessionId() + " - rid = {} (re-sending)", rid);
                        // Resending the old response
                        resendResponse(br);
                    } else {
                        // not in sent responses, try alternatives: backlog and requestWindow
                        
                        final BoshResponse response = sentResponsesBacklog.lookup(rid);
                        if (response != null) {
                            LOGGER.warn("SID = " + getSessionId() + " - rid = {} - BOSH response retrieved from sentResponsesBacklog", rid);
                            resendResponse(br, rid, response);
                            return; // no error
                        }

                        // rid not in sent responses, nor backlog. check to see if rid is still in requests window
                        boolean inRequestsWindow = requestsWindow.containsRid(rid);
                        if (!inRequestsWindow) {
                            if (LOGGER.isWarnEnabled()) {
                                final String sentRids = logSentResponsesBuffer();
                                LOGGER.warn("SID = " + getSessionId() + " - rid = {} - BOSH response not in buffer error - " + sentRids, rid);
                            }
                        } else {
                            if (LOGGER.isWarnEnabled()) {
                                final String sentRids = logSentResponsesBuffer();
                                LOGGER.warn("SID = " + getSessionId() + " - rid = {} - BOSH response still in requests window - " + sentRids, rid);
                            }
                        }
                        sendError(br, "item-not-found");
                    }
                }
                return;
            }
            // check for too many parallel requests
            final boolean terminate = "terminate".equals(boshOuterBody.getAttributeValue("type"));
            final boolean pause = boshOuterBody.getAttributeValue("pause") != null;
            final boolean bodyIsEmpty = boshOuterBody.getInnerElements().isEmpty();
            final int distinctRIDs = requestsWindow.getDistinctRIDs();
            
            if (distinctRIDs >= maxToleratedParallelRequests && !terminate && !pause) {
                LOGGER.warn("SID = " + getSessionId() + " - rid = {} - BOSH Overactivity: Too many simultaneous requests, max = {} " + logRIDSequence(), rid, maxToleratedParallelRequests);
                sendError(br, "policy-violation");
                return;
            }
            // check for new request comes early
            if (distinctRIDs + 1 == maxToleratedParallelRequests && !terminate && !pause && bodyIsEmpty) {
                final long millisSinceLastCalls = Math.abs(br.getTimestamp() - requestsWindow.getLatestAddionTimestamp());
                if (millisSinceLastCalls < pollingSeconds * 1000 && !rid.equals(requestsWindow.getLatestRID())) {
                    LOGGER.warn("SID = " + getSessionId() + " - rid = {} - BOSH Overactivity: Too frequent requests, millis since requests = {}, " + logRIDSequence(), rid, millisSinceLastCalls);
                    sendError(br, "policy-violation");
                    return;
                }
            }
            // check 
            if ((wait == 0 || hold == 0) && bodyIsEmpty) {
                final long millisBetweenEmptyReqs = Math.abs(br.getTimestamp() - latestEmptyPollingRequestTimestamp);
                if (millisBetweenEmptyReqs < pollingSeconds * 1000 && !rid.equals(requestsWindow.getLatestRID())) {
                    LOGGER.warn("SID = " + getSessionId() + " - rid = {} - BOSH Overactivity for polling: Too frequent requests, millis since requests = {}, " + logRIDSequence(), rid, millisBetweenEmptyReqs);
                    sendError(br, "policy-violation");
                    return;
                }
                latestEmptyPollingRequestTimestamp = br.getTimestamp();
            }

            queueRequest(br);
        }


        if (isClientAcknowledgements()) {
            synchronized (sentResponses) {
                if (boshOuterBody.getAttribute("ack") == null) {
                    // if there is no ack attribute present then the client confirmed it received all the responses to all the previous requests
                    // and we clear the cache
                    sentResponsesBacklog.addAll(sentResponses);
                    sentResponses.clear();
                } else if (!sentResponses.isEmpty()) {
                    // After receiving a request with an 'ack' value less than the 'rid' of the last request that it has already responded to,
                    // the connection manager MAY inform the client of the situation. In this case it SHOULD include a 'report' attribute set
                    // to one greater than the 'ack' attribute it received from the client, and a 'time' attribute set to the number of milliseconds
                    // since it sent the response associated with the 'report' attribute.
                    long ack = Long.parseLong(boshOuterBody.getAttributeValue("ack"));
                    if (ack < sentResponses.lastKey() && sentResponses.containsKey(ack + 1)) {
                        long delta = System.currentTimeMillis() - sentResponses.get(ack + 1).getTimestamp();
                        if (delta >= brokenConnectionReportTimeoutMillis) {
                            sendBrokenConnectionReport(ack + 1, delta);
                            return;
                        }
                    }
                }
            }
        }
        
        // we cannot pause if there are missing requests, this is tested with
        // br.getRid().equals(requestsWindow.lastKey()) && highestContinuousRid.equals(br.getRid())
        synchronized (requestsWindow) {
            final String pauseAttribute = boshOuterBody.getAttributeValue("pause");
            if (pauseAttribute != null && 
                    rid.equals(requestsWindow.getLatestRID()) && 
                    rid.equals(requestsWindow.getHighestContinuousRid())) {
                int pause;
                try {
                    pause = Integer.parseInt(pauseAttribute);
                } catch (NumberFormatException e) {
                    queueRequest(br);
                    sendError("bad-request");
                    return;
                }
                pause = Math.max(0, pause);
                pause = Math.min(pause, maxpauseSeconds);
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
            LOGGER.debug("SID = " + getSessionId() + " - writing merged response. stanzas merged = " + mergeCandidates.size());
            writeBoshResponse(mergedResponse);
            return;
        }

        // If there are more suspended enqueued requests than it is allowed by the BOSH 'hold' parameter,
        // than we release the oldest one by sending an empty response.
        if (requestsWindow.size() > hold) {
            writeBoshResponse(BoshStanzaUtils.EMPTY_BOSH_RESPONSE);
        }
    }

    public String logSentResponsesBuffer() {
        final StringBuffer logMsg = new StringBuffer("sent = [");
        for (Iterator<Long> iterator = sentResponses.keySet().iterator(); iterator.hasNext(); ) {
            Long sentRid = iterator.next();
            logMsg.append(sentRid);
            if (iterator.hasNext()) logMsg.append(", ");
        }
        logMsg.append("]");
        return logMsg.toString();
    }
    
    protected void respondToPause(int pause) {
        LOGGER.debug("SID = " + getSessionId() + " - Setting inactivity period to {}", pause);
        currentInactivitySeconds = pause;
        while (!requestsWindow.isEmpty()) {
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
                requestExpired(context);
            }

            public void onStartAsync(AsyncEvent event) throws IOException {
                // ignore
            }

            public void onError(AsyncEvent event) throws IOException {
                handleAsyncEventError(event);
            }

            public void onComplete(AsyncEvent event) throws IOException {
                // ignore
            }
        });
    }

    protected void handleAsyncEventError(AsyncEvent event) {
        final ServletRequest suppliedRequest = event.getSuppliedRequest();
        final ServletResponse suppliedResponse = event.getSuppliedResponse();
        BoshRequest boshRequest = (BoshRequest)suppliedRequest.getAttribute(BOSH_REQUEST_ATTRIBUTE);
        BoshResponse boshResponse = (BoshResponse)suppliedRequest.getAttribute(BOSH_RESPONSE_ATTRIBUTE);

        // works at least for jetty:
        final Exception exceptionObject = (Exception)suppliedRequest.getAttribute("javax.servlet.error.exception");
        final Throwable throwable = event.getThrowable() != null ? event.getThrowable() : exceptionObject;

        final Long rid = boshRequest == null ? null : boshRequest.getRid();
        LOGGER.warn("SID = " + getSessionId() + " - JID = " + getInitiatingEntity() + " - RID = " + rid + " - async error on event ", event.getClass(), throwable);
    }

    protected void resendResponse(BoshRequest br) {
        final Long rid = br.getRid();
        BoshResponse boshResponse = sentResponses.get(rid);
        resendResponse(br, rid, boshResponse);
    }

    protected void resendResponse(BoshRequest br, Long rid, BoshResponse boshResponse) {
        if (boshResponse == null) {
            LOGGER.debug("SID = " + getSessionId() + " - rid = {} - BOSH response could not (no longer) be retrieved for resending.", rid);
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SID = " + getSessionId() + " - rid = {} - BOSH writing response (resending): {}", rid, new String(boshResponse.getContent()));
        }

        try {
            final AsyncContext asyncContext = saveResponse(br, boshResponse);
            asyncContext.dispatch();
        } catch (Exception e) {
            LOGGER.warn("SID = " + getSessionId() + " - exception in async processing", e);
        }
        setLatestWriteTimestamp();
        updateInactivityChecker();
    }

    protected BoshResponse getBoshResponse(Stanza stanza, Long ack) {
        if (ack != null) {
            stanza = BoshStanzaUtils.addAttribute(stanza, "ack", ack.toString());
        }
        byte[] content = new Renderer(stanza).getComplete().getBytes(UTF8_CHARSET);
        return new BoshResponse(contentType, content);
    }

    protected void queueRequest(BoshRequest br) {
        requestsWindow.queueRequest(br);
        updateInactivityChecker();
    }

    protected AsyncContext saveResponse(final BoshRequest boshRequest, final BoshResponse boshResponse) {
        if (boshResponse == null) throw new IllegalArgumentException("boshResponse cannot be null");
        final HttpServletRequest request = boshRequest.getHttpServletRequest();
        final AsyncContext asyncContext = request.getAsyncContext();
        request.setAttribute(BOSH_RESPONSE_ATTRIBUTE, boshResponse);
        return asyncContext;
    }
}
