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

import javax.servlet.http.HttpServletRequest;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the session state
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshBackedSessionContext extends AbstractSessionContext implements StanzaWriter {

    private final static Logger LOGGER = LoggerFactory.getLogger(BoshBackedSessionContext.class);

    private final BoshHandler boshHandler;

    private final int inactivity = 60;

    private final int polling = 15;

    private final int requests = 2;

    private String boshVersion = "1.9";

    private String contentType = BoshServlet.XML_CONTENT_TYPE;

    private int wait = 60;

    private int hold = 1;

    private Queue<HttpServletRequest> requestQueue;

    private Queue<BoshResponse> delayedResponseQueue;

    /**
     * Creates a new context for a session
     * @param serverRuntimeContext
     * @param boshHandler
     */
    public BoshBackedSessionContext(BoshHandler boshHandler, ServerRuntimeContext serverRuntimeContext) {
        super(serverRuntimeContext, new SessionStateHolder());
        sessionStateHolder.setState(SessionState.INITIATED);
        this.boshHandler = boshHandler;
        requestQueue = new LinkedList<HttpServletRequest>();
        delayedResponseQueue = new LinkedList<BoshResponse>();
    }

    public SessionStateHolder getStateHolder() {
        return sessionStateHolder;
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void setIsReopeningXMLStream() {
    }

    public void write(Stanza stanza) {
        write(getResponse(stanza));
    }

    public void write(BoshResponse resp) {
        HttpServletRequest req = requestQueue.poll();
        if (req == null) {
            delayedResponseQueue.offer(resp);
            return;
        }
        Continuation continuation = ContinuationSupport.getContinuation(req);
        continuation.setAttribute("response", resp);
        continuation.resume();
    }

    public void close() {
        LOGGER.info("session will be closed now");
    }

    public void switchToTLS() {
        // BOSH cannot switch dynamically,
        // SSL can be enabled/disabled in BoshEndpoint#setSSLEnabled()
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setWait(int wait) {
        this.wait = Math.min(wait, this.wait);
    }

    public int getWait() {
        return wait;
    }

    public void setHold(int hold) {
        this.hold = Math.min(hold, this.hold);
    }

    public int getHold() {
        return hold;
    }

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

    public String getBoshVersion() {
        return boshVersion;
    }

    public int getInactivity() {
        return inactivity;
    }

    public int getPolling() {
        return polling;
    }

    public int getRequests() {
        return requests;
    }

    synchronized public void requestExpired(HttpServletRequest req) {
        for (;;) {
            HttpServletRequest r = requestQueue.peek();
            if (r == null) {
                return;
            }
            write(boshHandler.getEmptyStanza());
            if (r == req) {
                return;
            }
        }
    }

    public void addRequest(HttpServletRequest req) {
        Continuation continuation = ContinuationSupport.getContinuation(req);
        continuation.setTimeout(wait * 1000);
        continuation.suspend();
        continuation.setAttribute("session", this);
        requestQueue.offer(req);

        BoshResponse resp = delayedResponseQueue.poll();
        if (resp != null) {
            write(resp);
            return;
        }

        if (requestQueue.size() > hold) {
            write(boshHandler.getEmptyStanza());
        }
    }

    public BoshResponse getResponse(Stanza stanza) {
        byte[] content = new Renderer(stanza).getComplete().getBytes();
        return new BoshResponse(contentType, content);
    }

}
