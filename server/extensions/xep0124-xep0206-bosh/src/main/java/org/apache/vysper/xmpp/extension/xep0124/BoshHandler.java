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

import javax.servlet.http.HttpServletRequest;

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
 * This class is thread safe.
 * Concurrent BOSH clients can be handled simultaneously by this class safely. 
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoshHandler.class);

    private ServerRuntimeContext serverRuntimeContext;

    private Map<String, BoshBackedSessionContext> sessions;

    public BoshHandler() {
        sessions = new ConcurrentHashMap<String, BoshBackedSessionContext>();
    }

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /**
     * Processes BOSH stanzas concurrently
     * @param httpContext
     * @param stanza
     */
    public void process(HttpServletRequest req, Stanza stanza) {
        if (!stanza.getNamespaceURI().equalsIgnoreCase(NamespaceURIs.XEP0124_BOSH)) {
            LOGGER.error("Invalid namespace for body wrapper '{}', must be '{}'!", stanza.getNamespaceURI(),
                    NamespaceURIs.XEP0124_BOSH);
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
                createSession(req, stanza);
            } catch (IOException e) {
                LOGGER.error("Exception thrown while processing the session creation request", e);
                return;
            }
        } else {
            BoshBackedSessionContext session = sessions.get(stanza.getAttributeValue("sid"));
            if (session == null) {
                LOGGER.warn("Received an invalid 'sid'!");
                return;
            }
            synchronized (session) {
                session.addRequest(req);
                processSession(session, stanza);
            }
        }
    }
    
    private void processSession(BoshBackedSessionContext session, Stanza stanza) {
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
                session.write0(getRestartResponseStanza());
            } else {
                // any other request
                for (XMLElement element : stanza.getInnerElements()) {
                    processStanza(session, element);
                }
            }
        }
    }

    private void processStanza(BoshBackedSessionContext session, XMLElement element) {
        Stanza innerStanza;
        if (element instanceof Stanza) {
            innerStanza = (Stanza) element;
        } else {
            innerStanza = new Stanza(element.getNamespaceURI(), element.getName(), element.getNamespacePrefix(),
                    element.getAttributes(), element.getInnerFragments());
        }
        serverRuntimeContext.getStanzaProcessor().processStanza(serverRuntimeContext, session, innerStanza,
                session.getStateHolder());
    }

    private void createSession(HttpServletRequest req, Stanza stanza) throws IOException {
        BoshBackedSessionContext session = new BoshBackedSessionContext(this, serverRuntimeContext);
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
            session.setBoshVersion(ver);
        }
        session.addRequest(req);
        sessions.put(session.getSessionId(), session);

        session.write0(getSessionCreationStanza(session));
    }

    public Stanza getSessionCreationStanza(BoshBackedSessionContext session) {
        StanzaBuilder body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        body.addAttribute("wait", Integer.toString(session.getWait()));
        body.addAttribute("inactivity", Integer.toString(session.getInactivity()));
        body.addAttribute("polling", Integer.toString(session.getPolling()));
        body.addAttribute("requests", Integer.toString(session.getRequests()));
        body.addAttribute("hold", Integer.toString(session.getHold()));
        body.addAttribute("sid", session.getSessionId());
        body.addAttribute("ver", session.getBoshVersion());
        body.addAttribute("from", session.getServerJID().getFullQualifiedName());

        Stanza features = new ServerResponses().getFeaturesForAuthentication(serverRuntimeContext.getServerFeatures()
                .getAuthenticationMethods());
        body.addPreparedElement(features);
        return body.build();
    }

    public Stanza getEmptyStanza() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        return stanzaBuilder.build();
    }

    public Stanza wrapStanza(Stanza stanza) {
        StanzaBuilder body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        body.addPreparedElement(stanza);
        return body.build();
    }
    
    public Stanza mergeStanzas(Stanza stanza1, Stanza stanza2) {
        if (stanza1 == null && stanza2 == null) {
            return null;
        }
        if (stanza1 == null) {
            return stanza2;
        }
        if (stanza2 == null) {
            return stanza1;
        }
        StanzaBuilder body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        for (XMLElement element : stanza1.getInnerElements()) {
            body.addPreparedElement(element);
        }
        for (XMLElement element : stanza2.getInnerElements()) {
            body.addPreparedElement(element);
        }
        return body.build();
    }
    
    private Stanza getRestartResponseStanza() {
        Stanza features = new ServerResponses().getFeaturesForSession();
        return wrapStanza(features);
    }

}
