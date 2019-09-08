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
package org.apache.vysper.xmpp.protocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.modules.extension.xep0077_inbandreg.InBandRegistrationHandler;
import org.apache.vysper.xmpp.protocol.exception.TLSException;
import org.apache.vysper.xmpp.protocol.worker.AuthenticatedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.EncryptedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.EncryptionStartedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.EndOrClosedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.InitiatedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.StartedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.UnconnectedProtocolWorker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * responsible for high-level XMPP protocol logic for client-server sessions
 * determines start, end and jabber conditions.
 * reads the stream and cuts it into stanzas,
 * holds state and invokes stanza execution,
 * separates stream reading from actual execution.
 * stateless.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ProtocolWorker implements StanzaProcessor {

    final Logger logger = LoggerFactory.getLogger(ProtocolWorker.class);

    private final Map<SessionState, StateAwareProtocolWorker> stateWorker = new HashMap<SessionState, StateAwareProtocolWorker>();

    private final ResponseWriter responseWriter = new ResponseWriter();

    public ProtocolWorker(StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory) {

        stateWorker.put(SessionState.UNCONNECTED, new UnconnectedProtocolWorker(stanzaHandlerExecutorFactory));
        stateWorker.put(SessionState.INITIATED, new InitiatedProtocolWorker(stanzaHandlerExecutorFactory));
        stateWorker.put(SessionState.STARTED, new StartedProtocolWorker(stanzaHandlerExecutorFactory));
        stateWorker.put(SessionState.ENCRYPTION_STARTED, new EncryptionStartedProtocolWorker(stanzaHandlerExecutorFactory));
        stateWorker.put(SessionState.ENCRYPTED, new EncryptedProtocolWorker(stanzaHandlerExecutorFactory));
        stateWorker.put(SessionState.AUTHENTICATED, new AuthenticatedProtocolWorker(stanzaHandlerExecutorFactory));
        stateWorker.put(SessionState.ENDED, new EndOrClosedProtocolWorker(stanzaHandlerExecutorFactory));
        stateWorker.put(SessionState.CLOSED, new EndOrClosedProtocolWorker(stanzaHandlerExecutorFactory));
    }

    /**
     * executes the handler for a stanza, handles Protocol exceptions.
     * also writes a response, if the handler implements ResponseStanzaContainer
     * @param serverRuntimeContext
     * @param sessionContext
     * @param stanza
     * @param sessionStateHolder
     */
    public void processStanza(ServerRuntimeContext serverRuntimeContext, InternalSessionContext sessionContext, Stanza stanza,
							  SessionStateHolder sessionStateHolder) {
        if (stanza == null)
            throw new RuntimeException("cannot process NULL stanzas");

        StanzaHandler stanzaHandler = serverRuntimeContext.getHandler(stanza);
        if (stanzaHandler == null) {
            responseWriter.handleUnsupportedStanzaType(sessionContext, stanza);
            return;
        }
        if (sessionContext == null && stanzaHandler.isSessionRequired()) {
            throw new IllegalStateException("handler requires session context");
        }

        StateAwareProtocolWorker stateAwareProtocolWorker = stateWorker.get(sessionContext.getState());
        if (stateAwareProtocolWorker == null) {
            throw new IllegalStateException("no protocol worker for state " + sessionContext.getState().toString());
        }

        // check as of RFC3920/4.3:
        if (sessionStateHolder.getState() != SessionState.AUTHENTICATED) {
            // is not authenticated...
            if (XMPPCoreStanza.getWrapper(stanza) != null
                    && !(InBandRegistrationHandler.class.isAssignableFrom(stanzaHandler.unwrapType()))) {
                // ... and is a IQ/PRESENCE/MESSAGE stanza!
                responseWriter.handleNotAuthorized(sessionContext, stanza);
                return;
            }
        }

        Entity from = stanza.getFrom();
        if(sessionContext.isServerToServer()) {
            XMPPCoreStanza coreStanza = XMPPCoreStanza.getWrapper(stanza);
            
            if(coreStanza != null) {
                // stanza must come from the origin server
                if(from == null) {
                    Stanza errorStanza = ServerErrorResponses.getStanzaError(StanzaErrorCondition.UNKNOWN_SENDER,
                            coreStanza, StanzaErrorType.MODIFY, "Missing from attribute", null, null);
                    sessionContext.getResponseWriter().write(errorStanza);
                    return;
                } else if(!EntityUtils.isAddressingServer(sessionContext.getInitiatingEntity(), from)) {
                    // make sure the from attribute refers to the correct remote server
                    
                        Stanza errorStanza = ServerErrorResponses.getStanzaError(StanzaErrorCondition.UNKNOWN_SENDER,
                                coreStanza, StanzaErrorType.MODIFY, "Incorrect from attribute", null, null);
                    sessionContext.getResponseWriter().write(errorStanza);
                    return;
                }
                
                Entity to = stanza.getTo();
                if(to == null) {
                    // TODO what's the appropriate error? StreamErrorCondition.IMPROPER_ADDRESSING?
                    Stanza errorStanza = ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST,
                            coreStanza, StanzaErrorType.MODIFY, "Missing to attribute", null, null);
                    sessionContext.getResponseWriter().write(errorStanza);
                    return;                    
                } else if(!EntityUtils.isAddressingServer(serverRuntimeContext.getServerEntity(), to)) {
                    // TODO what's the appropriate error? StreamErrorCondition.IMPROPER_ADDRESSING?
                    Stanza errorStanza = ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST,
                            coreStanza, StanzaErrorType.MODIFY, "Invalid to attribute", null, null);
                    sessionContext.getResponseWriter().write(errorStanza);
                    return;                    
                    
                }

                // rewrite namespace
                stanza = StanzaBuilder.rewriteNamespace(stanza, NamespaceURIs.JABBER_SERVER, NamespaceURIs.JABBER_CLIENT);
            }                
        } else {
            // make sure that 'from' (if present) matches the bare authorized entity
            // else respond with a stanza error 'unknown-sender'
            // see rfc3920_draft-saintandre-rfc3920bis-04.txt#8.5.4
            if (from != null && sessionContext.getInitiatingEntity() != null) {
                Entity fromBare = from.getBareJID();
                Entity initiatingEntity = sessionContext.getInitiatingEntity();
                if (!initiatingEntity.equals(fromBare)) {
                    responseWriter.handleWrongFromJID(sessionContext, stanza);
                    return;
                }
            }
            // make sure that there is a bound resource entry for that from's resource id attribute!
            if (from != null && from.getResource() != null) {
                List<String> boundResources = sessionContext.getServerRuntimeContext().getResourceRegistry()
                        .getBoundResources(from, false);
                if (boundResources.size() == 0) {
                    responseWriter.handleWrongFromJID(sessionContext, stanza);
                    return;
                }
            }
            // make sure that there is a full from entity given in cases where more than one resource is bound
            // in the same session.
            // see rfc3920_draft-saintandre-rfc3920bis-04.txt#8.5.4
            if (from != null && from.getResource() == null) {
                List<String> boundResources = sessionContext.getServerRuntimeContext().getResourceRegistry()
                        .getResourcesForSession(sessionContext);
                if (boundResources.size() > 1) {
                    responseWriter.handleWrongFromJID(sessionContext, stanza);
                    return;
                }
            }
        }
        
        try {
            stateAwareProtocolWorker.processStanza(serverRuntimeContext, sessionContext, sessionStateHolder, stanza, stanzaHandler);
        } catch (Exception e) {
            logger.error("error executing handler {} with stanza {}", stanzaHandler.getClass().getName(),
                    DenseStanzaLogRenderer.render(stanza));
            logger.debug("error executing handler exception: ", e);
        }
    }

    public void processTLSEstablished(InternalSessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        processTLSEstablishedInternal(sessionContext, sessionStateHolder, responseWriter);
    }

    static void processTLSEstablishedInternal(InternalSessionContext sessionContext, SessionStateHolder sessionStateHolder,
											  ResponseWriter responseWriter) {
        if (sessionContext.getState() != SessionState.ENCRYPTION_STARTED) {
            responseWriter.handleProtocolError(new TLSException(), sessionContext, null);
            return;
        }
        sessionStateHolder.setState(SessionState.ENCRYPTED);
        sessionContext.setIsReopeningXMLStream();
    }
}
