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

import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;
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

    private final int inactivity = 60;

    private final int polling = 15;

    private int requests = 2;

    private String boshVersion = "1.9";

    private String contentType = BoshServlet.XML_CONTENT_TYPE;

    private int wait = 60;

    private int hold = 1;
    
    /*
     * The highest RID that can be read and processed, this is the highest (rightmost) contiguous RID.
     * The requests from the client can come theoretically with missing updates:
     * rid_1, rid_2, rid_4 (missing rid_3, highestReadRid is rid_2)
     */
    private Long highestReadRid = null;
    
    private Long currentProcessingRequest = null;
    
    private BoshRequest latestEmptyPollingRequest = null;
    
    /*
     * Keeps the suspended HTTP requests (does not respond to them) until the server has an asynchronous message
     * to send to the client. (Comet HTTP Long Polling technique - described in XEP-0124)
     * 
     * The BOSH requests are sorted by their RIDs.
     */
    private SortedMap<Long, BoshRequest> requestsWindow;

    /*
     * Keeps the asynchronous messages sent from server that cannot be delivered to the client because there are
     * no available HTTP requests to respond to (requestsWindow is empty).
     */
    private Queue<Stanza> delayedResponseQueue;

    /**
     * Creates a new context for a session
     * @param boshHandler
     * @param serverRuntimeContext
     */
    public BoshBackedSessionContext(BoshHandler boshHandler, ServerRuntimeContext serverRuntimeContext) {
        super(serverRuntimeContext, new SessionStateHolder());

        // in BOSH we jump directly to the encrypted state
        sessionStateHolder.setState(SessionState.ENCRYPTED);

        this.boshHandler = boshHandler;
        requestsWindow = new TreeMap<Long, BoshRequest>();
        delayedResponseQueue = new LinkedList<Stanza>();
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
        write0(boshHandler.wrapStanza(stanza));
    }

    /**
     * Writes a BOSH response (that is wrapped in a &lt;body/&gt; element) if there are available HTTP requests
     * to respond to, otherwise the response is queued to be sent later (when a HTTP request will be available).
     * <p>
     * (package access)
     * 
     * @param response The BOSH response to write
     */
    void write0(Stanza response) {
        BoshRequest req;
        if (requestsWindow.isEmpty() || requestsWindow.firstKey() > highestReadRid) {
            delayedResponseQueue.offer(response);
            return;
        } else {
            req = requestsWindow.remove(requestsWindow.firstKey());
        }
        BoshResponse boshResponse = getBoshResponse(response, req.getRid().equals(highestReadRid) ? null : highestReadRid);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("BOSH writing response: {}", new String(boshResponse.getContent()));
        }
        Continuation continuation = ContinuationSupport.getContinuation(req.getHttpServletRequest());
        continuation.setAttribute("response", boshResponse);
        continuation.resume();
    }
    
    /**
     * Writes an error to the client and closes the connection
     * @param condition the error condition
     */
    synchronized public void error(String condition) {
        if (!requestsWindow.isEmpty()) {
            BoshRequest req = requestsWindow.remove(requestsWindow.firstKey());
            Stanza stanza = boshHandler.getTerminateResponse();
            stanza = boshHandler.addAttribute(stanza, "condition", condition);
            BoshResponse boshResponse = getBoshResponse(stanza, null);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("BOSH writing response: {}", new String(boshResponse.getContent()));
            }
            Continuation continuation = ContinuationSupport.getContinuation(req.getHttpServletRequest());
            continuation.setAttribute("response", boshResponse);
            continuation.resume();
        }
        close();
    }

    /*
     * Terminates the BOSH session
     */
    synchronized public void close() {
        // respond to all the queued HTTP requests with empty responses
        while (!requestsWindow.isEmpty()) {
            write0(boshHandler.getEmptyResponse());
        }
        
        serverRuntimeContext.getResourceRegistry().unbindSession(this);
        sessionStateHolder.setState(SessionState.CLOSED);
        
        LOGGER.info("BOSH session {} closed", getSessionId());
    }

    public void switchToTLS() {
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
     * Setter for the highest version of the BOSH protocol that the connection manager supports, or the version
     * specified by the client in its request, whichever is lower.
     * @param version the BOSH version
     */
    public void setBoshVersion(String version) {
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
    synchronized private void requestExpired(Continuation continuation) {
        BoshRequest req = (BoshRequest) continuation.getAttribute("request");
        if (req == null) {
            LOGGER.warn("Continuation expired without having an associated request!");
            return;
        }
        while (!requestsWindow.isEmpty() && requestsWindow.firstKey() <= req.getRid()) {
            write0(boshHandler.getEmptyResponse());
        }
    }

    /**
     * Suspends and enqueues an HTTP request to be used later when an asynchronous message needs to be sent from
     * the connection manager to the BOSH client.
     * 
     * @param req the HTTP request
     */
    public void insertRequest(BoshRequest br) {
        if (highestReadRid != null && highestReadRid + requests < br.getRid()) {
            LOGGER.warn("BOSH received RID greater than the permitted window of concurrent requests");
            error("item-not-found");
            return;
        }
        if (highestReadRid != null && br.getRid() <= highestReadRid || requestsWindow.containsKey(br.getRid())) {
            // TODO: return the old response
            return;
        }
        if (requestsWindow.size() + 1 > requests && !"terminate".equals(br.getBody().getAttributeValue("type"))
                && br.getBody().getAttributeValue("pause") == null) {
            LOGGER.warn("BOSH Overactivity: Too many simultaneous requests");
            error("policy-violation");
            return;
        }
        if (requestsWindow.size() + 1 == requests && !"terminate".equals(br.getBody().getAttributeValue("type"))
                && br.getBody().getAttributeValue("pause") == null && br.getBody().getInnerElements().isEmpty()) {
            if (!requestsWindow.isEmpty()
                    && br.getTimestamp() - requestsWindow.get(requestsWindow.lastKey()).getTimestamp() < polling * 1000) {
                LOGGER.warn("BOSH Overactivity: Too frequent requests");
                error("policy-violation");
                return;
            }
        }
        if ((wait == 0 || hold == 0) && br.getBody().getInnerElements().isEmpty()) {
            if (latestEmptyPollingRequest != null && br.getTimestamp() - latestEmptyPollingRequest.getTimestamp() < polling * 1000) {
                LOGGER.warn("BOSH Overactivity for polling: Too frequent requests");
                error("policy-violation");
                return;
            }
            latestEmptyPollingRequest = br;
        }
        Continuation continuation = ContinuationSupport.getContinuation(br.getHttpServletRequest());
        continuation.setTimeout(wait * 1000);
        continuation.suspend();
        continuation.setAttribute("request", br);
        requestsWindow.put(br.getRid(), br);
        if (highestReadRid == null) {
            highestReadRid = br.getRid();
        }
        for (;;) {
            // update the highestAcknowledgedRid to the latest value
            // it is possible to have higher RIDs than the highestAcknowledgedRid with a gap between them (e.g. lost client request)
            if (requestsWindow.containsKey(highestReadRid + 1)) {
                highestReadRid++;
            } else {
                break;
            }
        }

        // listen the continuation to be notified when the request expires
        continuation.addContinuationListener(new ContinuationListener() {

            public void onTimeout(Continuation continuation) {
                requestExpired(continuation);
            }

            public void onComplete(Continuation continuation) {
                // ignore
            }

        });

        // If there are delayed responses waiting to be sent to the BOSH client, then we wrap them all in
        // a <body/> element and send them as a HTTP response to the current HTTP request.
        Stanza delayedResponse;
        Stanza mergedResponse = null;
        while ((delayedResponse = delayedResponseQueue.poll()) != null) {
            mergedResponse = boshHandler.mergeResponses(mergedResponse, delayedResponse);
        }
        if (mergedResponse != null) {
            write0(mergedResponse);
            return;
        }

        // If there are more suspended enqueued requests than it is allowed by the BOSH 'hold' parameter,
        // than we release the oldest one by sending an empty response.
        if (requestsWindow.size() > hold) {
            write0(boshHandler.getEmptyResponse());
        }
    }

    private BoshResponse getBoshResponse(Stanza stanza, Long ack) {
        if (ack != null) {
            stanza = boshHandler.addAttribute(stanza, "ack", ack.toString());
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
