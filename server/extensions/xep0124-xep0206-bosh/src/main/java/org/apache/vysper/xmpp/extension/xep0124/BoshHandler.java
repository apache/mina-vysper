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

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes the BOSH requests from the clients
 * <p>
 * This class is thread safe, it handles concurrent BOSH requests safely.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoshHandler.class);

    private ServerRuntimeContext serverRuntimeContext;

    private Map<String, BoshBackedSessionContext> sessions;
    
    private InactivityChecker inactivityChecker;

    public BoshHandler() {
        // The sessions are stored in a ConcurrentHashMap to maintain the "happens before relationship" memory consistency.
        // Although the operations on specific sessions are synchronized, the session creation and retrieval need the memory
        // consistency guarantee too.
        sessions = new ConcurrentHashMap<String, BoshBackedSessionContext>();
        inactivityChecker = new InactivityChecker();
        inactivityChecker.start();
    }

    
    /**
     * Getting for the {@link ServerRuntimeContext}
     * @return The current {@link ServerRuntimeContext}
     */
    public ServerRuntimeContext getServerRuntimeContext() {
        return serverRuntimeContext;
    }

    /**
     * Setter for the {@link ServerRuntimeContext}
     * @param serverRuntimeContext
     */
    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /**
     * Processes BOSH requests
     * @param httpRequest the HTTP request
     * @param body the decoded BOSH request
     */
    public void process(HttpServletRequest httpRequest, Stanza body) {
        if (!body.getNamespaceURI().equalsIgnoreCase(NamespaceURIs.XEP0124_BOSH)) {
            LOGGER.error("Invalid namespace for body wrapper '{}', must be '{}'!", body.getNamespaceURI(),
                    NamespaceURIs.XEP0124_BOSH);
            return;
        }
        if (!body.getName().equalsIgnoreCase("body")) {
            LOGGER.error("Invalid body wrapper '{}'!", body.getName());
            return;
        }
        if (body.getAttribute("rid") == null) {
            LOGGER.error("Invalid request that does not have a request identifier (rid) attribute!");
            return;
        }
        BoshRequest br = new BoshRequest(httpRequest, body, Long.parseLong(body.getAttributeValue("rid")));
        if (body.getAttribute("sid") == null) {
            // the session creation request (first request) does not have a "sid" attribute
            try {
                createSession(br);
            } catch (IOException e) {
                LOGGER.error("Exception thrown while processing the session creation request", e);
                return;
            }
        } else {
            final String sid = body.getAttributeValue("sid");
            BoshBackedSessionContext session = null;
            if (sid != null) session = sessions.get(sid);
            if (session == null) {
                LOGGER.warn("Received an invalid sid = '{}', should terminating connection", sid);
                // TODO terminate connection
                return;
            }
            synchronized (session) {
                session.insertRequest(br);
                for (;;) {
                    // When a request from the user comes in, it is possible that the request fills a gap
                    // created by previous lost request, and it could be possible to process more than the current request
                    // continuing with all the adjacent requests.
                    br = session.getNextRequest();
                    if (br == null) {
                        break;
                    }
                    processSession(session, br);
                }
            }
        }
    }
    
    private void processSession(BoshBackedSessionContext session, BoshRequest br) {
        final Stanza stanza = br.getBody();
        if (session.getState() == SessionState.ENCRYPTED) {
            if (stanza.getInnerElements().isEmpty()) {
                // session needs authentication
                return;
            }
            for (XMLElement element : stanza.getInnerElements()) {
                if (element.getNamespaceURI().equals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)) {
                    processStanza(session, element);
                }
            }
        } else if (session.getState() == SessionState.AUTHENTICATED) {
            if ("true".equals(stanza.getAttributeValue(NamespaceURIs.URN_XMPP_XBOSH, "restart"))) {
                // restart request
                session.writeBoshResponse(BoshStanzaUtils.RESTART_BOSH_RESPONSE);
            } else {
                // any other request
                for (XMLElement element : stanza.getInnerElements()) {
                    processStanza(session, element);
                }
                
                // if the client solicited the session termination
                if ("terminate".equals(stanza.getAttributeValue("type"))) {
                    terminateSession(session);
                }
            }
        } else {
            LOGGER.debug("processing session while in state = " + session.getState());
        }
    }

    private void terminateSession(BoshBackedSessionContext session) {
        sessions.remove(session.getSessionId());
        session.writeBoshResponse(BoshStanzaUtils.TERMINATE_BOSH_RESPONSE);
        session.close();
    }

    private void processStanza(BoshBackedSessionContext session, XMLElement element) {
        Stanza stanza;
        if (element instanceof Stanza) {
            stanza = (Stanza) element;
        } else {
            stanza = new Stanza(element.getNamespaceURI(), element.getName(), element.getNamespacePrefix(),
                    element.getAttributes(), element.getInnerFragments());
        }
        serverRuntimeContext.getStanzaProcessor().processStanza(serverRuntimeContext, session, stanza,
                session.getStateHolder());
    }

    private void createSession(BoshRequest br) throws IOException {
        final Stanza stanza = br.getBody();

        BoshBackedSessionContext session = new BoshBackedSessionContext(this, serverRuntimeContext, inactivityChecker);
        
        final String contentAttribute = stanza.getAttributeValue("content");
        if (contentAttribute != null) session.setContentType(contentAttribute);
        
        String waitAttribute = null;
        try {
            waitAttribute = stanza.getAttributeValue("wait");
            final int wait = Integer.parseInt(waitAttribute);
            session.setWait(wait);
        } catch (NumberFormatException e) {
            LOGGER.warn("wait value is expected to be an Integer, not {}", waitAttribute);
        }

        final String holdAttribute = stanza.getAttributeValue("hold");
        try {
            int hold = Integer.parseInt(holdAttribute);
            session.setHold(hold);
        } catch (NumberFormatException e) {
            LOGGER.warn("hold value is expected to be an Integer, not {}", waitAttribute);
        }

        final String versionAttribute = stanza.getAttributeValue("ver");
        if (versionAttribute != null) {
            try {
                session.setBoshVersion(versionAttribute);
            } catch (NumberFormatException e) {
                LOGGER.warn("bosh version is expected to be of form nn.mm, not {}", waitAttribute);
            }
        }

        final String langAttribute = stanza.getAttributeValue(NamespaceURIs.XML, "lang");
        if (langAttribute != null) session.setXMLLang(langAttribute);
        
        if ("1".equals(stanza.getAttributeValue("ack"))) {
            session.setClientAcknowledgements(true);
        }
        session.insertRequest(br);
        sessions.put(session.getSessionId(), session);

        LOGGER.info("BOSH session created with session id = {}", session.getSessionId());

        session.writeBoshResponse(getSessionCreationResponse(session));
    }

    private Stanza getSessionCreationResponse(BoshBackedSessionContext session) {
        StanzaBuilder body = BoshStanzaUtils.createBoshStanzaBuilder();
        body.addAttribute("wait", Integer.toString(session.getWait()));
        body.addAttribute("inactivity", Integer.toString(session.getInactivity()));
        body.addAttribute("polling", Integer.toString(session.getPolling()));
        body.addAttribute("requests", Integer.toString(session.getRequests()));
        body.addAttribute("hold", Integer.toString(session.getHold()));
        body.addAttribute("sid", session.getSessionId());
        body.addAttribute("ver", session.getBoshVersion());
        body.addAttribute("from", session.getServerJID().getFullQualifiedName());
        body.addAttribute("secure", "true");
        body.addAttribute("maxpause", Integer.toString(session.getMaxPause()));
        
        // adding the ack attribute here is needed because when responding to o request with the same RID (as is the case here)
        // the ack would not be included on BoshBackedSessionContext#write0, but this first ack is required.
        body.addAttribute("ack", Long.toString(session.getHighestReadRid()));

        Stanza features = new ServerResponses().getFeaturesForAuthentication(serverRuntimeContext.getServerFeatures()
                .getAuthenticationMethods(), session);
        body.addPreparedElement(features);
        return body.build();

    }

    public Stanza addAttribute(Stanza stanza, String attributeName, String attributeValue) {
        StanzaBuilder stanzaBuilder = BoshStanzaUtils.createBoshStanzaBuilder();
        for (Attribute attr : stanza.getAttributes()) {
            stanzaBuilder.addAttribute(attr);
        }
        stanzaBuilder.addAttribute(attributeName, attributeValue);
        for (XMLElement element : stanza.getInnerElements()) {
            stanzaBuilder.addPreparedElement(element);
        }
        return stanzaBuilder.build();
    }
}
