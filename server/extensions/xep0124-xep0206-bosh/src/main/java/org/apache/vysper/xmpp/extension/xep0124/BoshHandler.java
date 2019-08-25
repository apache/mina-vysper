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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes the BOSH requests from the clients
 * <p>
 * This class is thread safe, it handles concurrent BOSH requests safely.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshHandler implements ServerRuntimeContextService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoshHandler.class);

    private ServerRuntimeContext serverRuntimeContext;
    
    private StanzaProcessor stanzaProcessor;

    private Map<String, BoshBackedSessionContext> sessions;
    
    private InactivityChecker inactivityChecker;

    public BoshHandler() {
        // The sessions are stored in a ConcurrentHashMap to maintain the "happens before relationship" memory consistency.
        // Although the operations on specific sessions are synchronized, the session creation and retrieval need the memory
        // consistency guarantee too.
        sessions = new ConcurrentHashMap<String, BoshBackedSessionContext>();
        inactivityChecker = new InactivityChecker(this);
        inactivityChecker.start();
    }

    
    /**
     * Getting for the {@link ServerRuntimeContext}
     * @return The current {@link ServerRuntimeContext}
     */
    public ServerRuntimeContext getServerRuntimeContext() {
        return serverRuntimeContext;
    }

    protected InactivityChecker getInactivityChecker() {
        return inactivityChecker;
    }

    /**
     * Setter for the {@link ServerRuntimeContext}
     * @param serverRuntimeContext
     */
    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }
    
    public void setStanzaProcessor(StanzaProcessor stanzaProcessor){
        this.stanzaProcessor = stanzaProcessor;
    }

    protected BoshBackedSessionContext createSessionContext() {
        return new BoshBackedSessionContext(serverRuntimeContext, stanzaProcessor, this, inactivityChecker);
    }

    /**
     * Processes incoming BOSH requests
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
        final long rid;
        try {
            rid = Long.parseLong(body.getAttributeValue("rid"));
        } catch (NumberFormatException e) {
            LOGGER.error("not a valid RID: " + body.getAttributeValue("rid"));
            // TODO handle properly by returning an error response
            throw new RuntimeException(e);
        }
        if (rid <= 0L) {
            LOGGER.warn("rid is not positive: " + rid);
            // TODO handle properly by returning an error response
            throw new RuntimeException("BOSH rid must be a positive, large number, not " + rid);
        }
        if (rid > 9007199254740991L) {
            LOGGER.warn("rid too large: " + rid);
            // continue anyway, this is not a problem with this implementation
        }
        BoshRequest newBoshRequest = new BoshRequest(httpRequest, body, rid);
        LOGGER.debug("SID = " + body.getAttributeValue("sid") + " - new BoshRequest created for RID = " + rid);

        // session creation request (first request). does not have a "sid" attribute
        if (body.getAttribute("sid") == null) {
            try {
                createSession(newBoshRequest);
            } catch (IOException e) {
                LOGGER.error("Exception thrown while processing the session creation request: " + e.getMessage());
                try {
                    final AsyncContext context = newBoshRequest.getHttpServletRequest().startAsync();
                    final ServletResponse response = context.getResponse();
                    if (response instanceof HttpServletResponse) {
                        HttpServletResponse httpServletResponse = (HttpServletResponse)response;
                        try {
                            httpServletResponse.sendError(400, "bad-request");
                            return;
                        } catch (IOException ioe) {
                            ioe.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    } else {
                        // create temporary session to be able to reuse the code
                        createSessionContext().sendError("bad-request");
                    }
                } catch (Exception exe) {
                    LOGGER.warn("exception in async processing", exe);
                }
            }
            return;
        } 

        // in-session request, now find the server-side session
        final String sid = body.getAttributeValue("sid");
        BoshBackedSessionContext session = null;
        if (sid != null) session = sessions.get(sid);
        if (session == null) {
            LOGGER.warn("Received an invalid sid = '{}', terminating connection", sid);
            try {
                final AsyncContext context = newBoshRequest.getHttpServletRequest().startAsync();
                // create temporary session to be able to reuse the code
                createSessionContext().sendError("invalid session id");
            } catch (Exception exe) {
                LOGGER.warn("exception in async processing", exe);
            }
            return;
        }
        
        // process request for the session
        synchronized (session) {
            session.insertRequest(newBoshRequest);
            processIncomingClientStanzas(session, newBoshRequest.getBody());
        }
    }
    
    protected void processIncomingClientStanzas(BoshBackedSessionContext session, Stanza stanza) {
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

    protected void terminateSession(BoshBackedSessionContext session) {
        removeSession(session.getSessionId());
        session.writeBoshResponse(BoshStanzaUtils.TERMINATE_BOSH_RESPONSE);
        session.close();
    }

    protected void processStanza(BoshBackedSessionContext session, XMLElement element) {
        Stanza stanza;
        if (element instanceof Stanza) {
            stanza = (Stanza) element;
        } else {
            stanza = new Stanza(element.getNamespaceURI(), element.getName(), element.getNamespacePrefix(),
                    element.getAttributes(), element.getInnerFragments());
        }
        stanzaProcessor.processStanza(serverRuntimeContext, session, stanza,
                session.getStateHolder());
    }

    protected void createSession(BoshRequest br) throws IOException {
        final Stanza stanza = br.getBody();

        if (stanza.getInnerElements().size() > 0) {
            // we can see this behavior when the server-side session is aborted, but 
            // the client keeps sending stanzas
            LOGGER.error("session creation request has content.");
            throw new IOException("session creation request has content.");
        }
        
        BoshBackedSessionContext session = createSessionContext();
        if (session.propagateSessionContext() && br.getHttpServletRequest() != null) {
            final HttpSession httpSession = br.getHttpServletRequest().getSession(true);
            httpSession.setAttribute(BoshBackedSessionContext.HTTP_SESSION_ATTRIBUTE, session);
        }
        
        final String contentAttribute = stanza.getAttributeValue("content");
        if (contentAttribute != null) session.setContentType(contentAttribute);

        String waitAttribute = stanza.getAttributeValue("wait");
        if (waitAttribute != null) {
            try {
                final int wait = Integer.parseInt(waitAttribute);
                session.setWait(wait);
            } catch (NumberFormatException e) {
                LOGGER.warn("wait value is expected to be an Integer, not {}", waitAttribute);
            }
        }

        final String holdAttribute = stanza.getAttributeValue("hold");
        if (holdAttribute != null) {
            try {
                int hold = Integer.parseInt(holdAttribute);
                session.setHold(hold);
            } catch (NumberFormatException e) {
                LOGGER.warn("hold value is expected to be an Integer, not {}", holdAttribute);
            }
        }

        final String versionAttribute = stanza.getAttributeValue("ver");
        if (versionAttribute != null) {
            try {
                session.setBoshVersion(versionAttribute);
            } catch (NumberFormatException e) {
                LOGGER.warn("bosh version is expected to be of form nn.mm, not {}", versionAttribute);
            }
        }

        final String langAttribute = stanza.getAttributeValue(NamespaceURIs.XML, "lang");
        if (langAttribute != null) session.setXMLLang(langAttribute);

        final String ackAttribute = stanza.getAttributeValue("ack");
        final boolean clientAcknowledgements = "1".equals(ackAttribute);
        session.setClientAcknowledgements(clientAcknowledgements);
        
        session.insertRequest(br);
        sessions.put(session.getSessionId(), session);

        if (LOGGER.isInfoEnabled()) {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("BOSH session created with session id = ").append(session.getSessionId());
            logMsg.append(", ver = ").append(session.getBoshVersion()).append(" (").append(versionAttribute).append(")");
            logMsg.append(", hold = ").append(session.getHold()).append(" (").append(holdAttribute).append(")");
            logMsg.append(", wait = ").append(session.getWait()).append(" (").append(waitAttribute).append(")");
            logMsg.append(", ack = ").append(session.isClientAcknowledgements()).append(" (").append(ackAttribute).append(")");
            LOGGER.info(logMsg.toString());
        }

        session.writeBoshResponse(getSessionCreationResponse(session));
    }

    public boolean removeSession(String sessionId) {
        return sessions.remove(sessionId) != null;
    }
    
    protected Stanza getSessionCreationResponse(BoshBackedSessionContext session) {
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
        body.addAttribute("ack", Long.toString(session.requestsWindow.getHighestContinuousRid()));

        Stanza features = new ServerResponses().getFeaturesForAuthentication(serverRuntimeContext.getServerFeatures()
                .getAuthenticationMethods(), session);
        body.addPreparedElement(features);
        return body.build();

    }

    public int getActiveSessionsCount() {
        return sessions.size();
    }

    public String getServiceName() {
        return this.getClass().getName();
    }
}
