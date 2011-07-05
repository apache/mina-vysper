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

import org.apache.vysper.xml.fragment.Attribute;
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
            BoshBackedSessionContext session = sessions.get(body.getAttributeValue("sid"));
            if (session == null) {
                LOGGER.warn("Received an invalid 'sid'!");
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
        if (session.getState() == SessionState.ENCRYPTED) {
            if (br.getBody().getInnerElements().isEmpty()) {
                // session needs authentication
                return;
            }
            for (XMLElement element : br.getBody().getInnerElements()) {
                if (element.getNamespaceURI().equals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)) {
                    processStanza(session, element);
                }
            }
        } else if (session.getState() == SessionState.AUTHENTICATED) {
            if ("true".equals(br.getBody().getAttributeValue(NamespaceURIs.URN_XMPP_XBOSH, "restart"))) {
                // restart request
                session.write0(getRestartResponse());
            } else {
                // any other request
                for (XMLElement element : br.getBody().getInnerElements()) {
                    processStanza(session, element);
                }
                
                // if the client solicited the session termination
                if ("terminate".equals(br.getBody().getAttributeValue("type"))) {
                    terminateSession(session);
                }
            }
        }
    }

    private void terminateSession(BoshBackedSessionContext session) {
        sessions.remove(session.getSessionId());
        session.write0(getTerminateResponse());
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
        BoshBackedSessionContext session = new BoshBackedSessionContext(this, serverRuntimeContext, inactivityChecker);
        if (br.getBody().getAttribute("content") != null) {
            session.setContentType(br.getBody().getAttributeValue("content"));
        }
        if (br.getBody().getAttribute("wait") != null) {
            int wait = Integer.parseInt(br.getBody().getAttributeValue("wait"));
            session.setWait(wait);
        }
        if (br.getBody().getAttribute("hold") != null) {
            int hold = Integer.parseInt(br.getBody().getAttributeValue("hold"));
            session.setHold(hold);
        }
        if (br.getBody().getAttribute("ver") != null) {
            String ver = br.getBody().getAttributeValue("ver");
            session.setBoshVersion(ver);
        }
        if (br.getBody().getAttribute(NamespaceURIs.XML, "lang") != null) {
            String lang = br.getBody().getAttributeValue(NamespaceURIs.XML, "lang");
            session.setXMLLang(lang);
        }
        if ("1".equals(br.getBody().getAttributeValue("ack"))) {
            session.setClientAcknowledgements(true);
        }
        session.insertRequest(br);
        sessions.put(session.getSessionId(), session);

        session.write0(getSessionCreationResponse(session));
    }

    private Stanza getSessionCreationResponse(BoshBackedSessionContext session) {
        StanzaBuilder body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
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

    /**
     * Creates an empty BOSH response.
     * <p>
     * The empty BOSH response looks like <code>&lt;body xmlns='http://jabber.org/protocol/httpbind'/&gt;</code>
     * @return the empty BOSH response
     */
    public Stanza getEmptyResponse() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        return stanzaBuilder.build();
    }

    /**
     * Creates a BOSH response by wrapping a stanza in a &lt;body/&gt; element
     * @param stanza the XMPP stanza to wrap
     * @return the BOSH response
     */
    public Stanza wrapStanza(Stanza stanza) {
        StanzaBuilder body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        body.addPreparedElement(stanza);
        return body.build();
    }
    
    /**
     * Creates a BOSH response by merging 2 other BOSH responses, this is useful when sending more than one message as
     * a response to a HTTP request.
     * @param response1 the first BOSH response to merge
     * @param response2 the second BOSH response to merge
     * @return the merged BOSH response
     */
    public Stanza mergeResponses(Stanza response1, Stanza response2) {
        if (response1 == null && response2 == null) {
            return null;
        }
        if (response1 == null) {
            return response2;
        }
        if (response2 == null) {
            return response1;
        }
        StanzaBuilder body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        for (XMLElement element : response1.getInnerElements()) {
            body.addPreparedElement(element);
        }
        for (XMLElement element : response2.getInnerElements()) {
            body.addPreparedElement(element);
        }
        return body.build();
    }
    
    private Stanza getRestartResponse() {
        Stanza features = new ServerResponses().getFeaturesForSession();
        return wrapStanza(features);
    }
    
    /**
     * Creates a session termination BOSH response
     * @return the termination BOSH body
    */
    public Stanza getTerminateResponse() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
        stanzaBuilder.addAttribute("type", "terminate");
        return stanzaBuilder.build();
    }
    
    /**
     * Adds a custom attribute to a BOSH body.
     * 
     * @param stanza the BOSH body
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @return a new BOSH body identical with the one provided except it also has the newly added attribute
     */
    public Stanza addAttribute(Stanza stanza, String attributeName, String attributeValue) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
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
