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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes the BOSH requests from the clients
 * <p>
 * This class is thread safe.
 * Concurrent BOSH clients can be handled simultaneously by this class safely. 
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BoshHandler.class);
    
    private static final String BOSH_NS = "http://jabber.org/protocol/httpbind";
    
    private ServerRuntimeContext serverRuntimeContext;
    
    private Map<String, BoshBackedSessionContext> sessions;
    
    public BoshHandler() {
        sessions = new ConcurrentHashMap<String, BoshBackedSessionContext>();
    }

    public void setServerRuntimeContext(
            ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /**
     * Processes BOSH stanzas concurrently
     * @param boshRequestContext
     * @param stanza
     */
    public void process(BoshRequestContext boshRequestContext, Stanza stanza) {
        if (!stanza.getNamespaceURI().equalsIgnoreCase(BOSH_NS)) {
            LOGGER.error("Invalid namespace for body wrapper '{}', should be '{}'!", stanza.getNamespaceURI(), BOSH_NS);
            return;
        }
        if (!stanza.getName().equalsIgnoreCase("body")) {
            LOGGER.error("Invalid body wrapper '{}'!", stanza.getName());
            return;
        }
        if (stanza.getAttribute("rid") == null) {
            LOGGER.error("Invalid request that does not have a request identifier (rid) attribute!");
            return;
        }
        
        if (stanza.getAttribute("sid") == null) {
            // the session creation request (first request) does not have a "sid" attribute
            try {
                createSession(boshRequestContext, stanza);
            } catch (IOException e) {
                LOGGER.error("Exception thrown while processing the session creation request", e);
                return;
            }
        } else {
//            handleSession(boshRequestContext, stanza);
        }
    }

    private void createSession(BoshRequestContext boshRequestContext,
            Stanza stanza) throws IOException {
        BoshBackedSessionContext session = new BoshBackedSessionContext(serverRuntimeContext);
        if (stanza.getAttribute("content") != null) {
            session.setContentType(stanza.getAttributeValue("content"));
        }
        if (stanza.getAttribute("wait") != null) {
            int wait = Integer.parseInt(stanza.getAttributeValue("wait"));
            session.setWait(wait);
        }
        if (stanza.getAttribute("hold") != null) {
            int hold = Integer.parseInt(stanza.getAttributeValue("hold"));
            session.setHold(hold);
        }
        if (stanza.getAttribute("ver") != null) {
            String ver = stanza.getAttributeValue("ver");
            session.setVer(ver);
        }
        sessions.put(session.getSessionId(), session);
        HttpServletResponse resp = boshRequestContext.getResponse();
        resp.setContentType(session.getContentType());
        String msg = new Renderer(getSessionCreationResponse(session)).getComplete();
        resp.setContentLength(msg.length());
        resp.addDateHeader("Date", System.currentTimeMillis());
        resp.addHeader("Access-control-allow-origin", "*");
        resp.addHeader("Access-control-allow-headers", "Content-Type");
        resp.getWriter().print(msg);
        resp.flushBuffer();
    }
    
    public Stanza getSessionCreationResponse(BoshBackedSessionContext session) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("body", BOSH_NS);
        stanzaBuilder.addAttribute("wait", Integer.toString(session.getWait()));
        stanzaBuilder.addAttribute("inactivity", Integer.toString(session.getInactivity()));
        stanzaBuilder.addAttribute("polling", Integer.toString(session.getPolling()));
        stanzaBuilder.addAttribute("requests", Integer.toString(session.getRequests()));
        stanzaBuilder.addAttribute("hold", Integer.toString(session.getHold()));
        stanzaBuilder.addAttribute("sid", session.getSessionId());
        stanzaBuilder.addAttribute("ver", session.getVer());
        stanzaBuilder.addAttribute("from", session.getServerJID().getFullQualifiedName());
        return stanzaBuilder.build();
    }

}
