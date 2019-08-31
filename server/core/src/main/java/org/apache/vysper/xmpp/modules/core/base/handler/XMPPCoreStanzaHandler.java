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
package org.apache.vysper.xmpp.modules.core.base.handler;

import java.util.List;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * foundation for the three core protocol stanzas: iq, message, presence
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class XMPPCoreStanzaHandler implements StanzaHandler {

    final static Logger logger = LoggerFactory.getLogger(XMPPCoreStanzaHandler.class);

    public boolean verify(Stanza stanza) {
        if (stanza == null)
            return false;

        boolean typeVerified = verifyType(stanza);
        boolean namespaceVerified = verifyNamespace(stanza);
        return typeVerified && namespaceVerified;
    }

    public boolean isSessionRequired() {
        return true;
    }

    protected abstract boolean verifyType(Stanza stanza);

    protected boolean verifyNamespace(Stanza stanza) {
        return NamespaceURIs.JABBER_CLIENT.equals(stanza.getNamespaceURI())
                || NamespaceURIs.JABBER_SERVER.equals(stanza.getNamespaceURI());
    }

    public void execute(Stanza anyStanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker) {
        XMPPCoreStanza stanza = XMPPCoreStanza.getWrapper(anyStanza);
        if (stanza == null)
            throw new IllegalArgumentException("can only handle core XMPP stanzas (iq, message, presence)");

        // type="error" is common to all stanza, check here some prerequisites
        Attribute typeAttribute = stanza.getAttribute("type");
        XMPPCoreStanza xmppCoreStanza = XMPPCoreStanza.getWrapper(stanza);
        if (xmppCoreStanza != null && typeAttribute != null) {
            String errorDescription = null;
            String type = typeAttribute.getValue();
            if (IQStanzaType.ERROR.value().equals(type)) {
                // assure, result contains zero or one element
                // rfc3920/9.2.3/7.
                if (!stanza.getVerifier().subElementPresent("error")) {
                    errorDescription = "stanza of type error must include an 'error' child";
                }
            } else {
                // assure, non-error result does not contain error
                // rfc3920/9.2.3/7. + rfc3920/9.3.1/3.
                if (stanza.getVerifier().subElementPresent("error")) {
                    errorDescription = "stanza which is not of type error must not include an 'error' child";
                }
            }

            // at this point, we are not allowed to respond with another error
            // we cannot really close the stream
            // we simply ignore it.
            /*
             * ResponseStanzaContainerImpl errorResponseContainer = new
             * ResponseStanzaContainerImpl(
             * ServerErrorResponses.getInstance().getErrorResponse(xmppCoreStanza,
             * StanzaErrorType.MODIFY, StanzaErrorCondition.BAD_REQUEST, errorDescription,
             * sessionContext.getXMLLang(), null) ); return errorResponseContainer;
             */
        }

        Entity to = stanza.getTo();
        if (sessionContext != null && sessionContext.isServerToServer() && to == null) {
            // "to" MUST be present for jabber:server
            stanzaBroker.writeToSession(ServerErrorResponses.getStreamError(StreamErrorCondition.IMPROPER_ADDRESSING,
                    stanza.getXMLLang(), "missing to attribute", null));
            return;
        }

        if (to != null) {
            // TODO ensure, that RFC3920 9.1.1 "If the value of the 'to' attribute is
            // invalid or cannot be contacted..." is enforced
        }

        List<Stanza> responseStanzas = executeCore(stanza, serverRuntimeContext, isOutboundStanza, sessionContext,
                stanzaBroker);
        if (responseStanzas == null) {
            return;
        }
        responseStanzas.forEach(stanzaBroker::writeToSession);
    }

    protected abstract List<Stanza> executeCore(XMPPCoreStanza stanza, ServerRuntimeContext serverRuntimeContext,
            boolean isOutboundStanza, SessionContext sessionContext, StanzaBroker stanzaBroker);

    /**
     * Extracts the from address either from the "from" attribute of the stanza, if
     * this isn't given retracts to using the address of the initiating entity plus
     * the resource of the sessionContext (if available).
     * 
     * A client might send a stanza without a 'from' attribute, if the sending (bare
     * or full) entity can be determined from the context. such a missing from is
     * determined here, if possible. for a formal discussion, see
     * RFC3921bis/Resource Binding/Binding multiple resources/From Addresses
     * 
     * @param stanza
     * @param sessionContext
     * @return The JID of the sender, either from the stanza or the context. A bare
     *         JID is returned if no, or more than one resource is bound.
     */
    public static Entity extractSenderJID(XMPPCoreStanza stanza, SessionContext sessionContext) {
        Entity from = stanza.getFrom();
        if (from == null) {
            from = new EntityImpl(sessionContext.getInitiatingEntity(), sessionContext.getServerRuntimeContext()
                    .getResourceRegistry().getUniqueResourceForSession(sessionContext));
        }
        return from;
    }

    /**
     * Extracts the from address either from the "from" attribute of the stanza, if
     * this isn't given retracts to using the address of the initiating entity plus
     * the resource of the sessionContext.
     * 
     * A client might send a stanza without a 'from' attribute, if the sending (bare
     * or full) entity can be determined from the context. such a missing from is
     * determined here, if possible. for a formal discussion, see
     * RFC3921bis/Resource Binding/Binding multiple resources/From Addresses
     * 
     * @param stanza
     * @param sessionContext
     * @return The JID of the sender, either from the stanza or the context. If
     *         there is no, or multiple resources bound, it returns null.
     */
    public static Entity extractUniqueSenderJID(XMPPCoreStanza stanza, SessionContext sessionContext) {
        Entity from = stanza.getFrom();
        if (from != null) {
            return from;
        }

        // Use the information stored within the context
        Entity initiatingEntity = sessionContext.getInitiatingEntity();
        if (initiatingEntity == null) {
            throw new RuntimeException("no 'from' attribute, and initiating entity not set");
        }

        String resourceId = sessionContext.getServerRuntimeContext().getResourceRegistry()
                .getUniqueResourceForSession(sessionContext);
        if (resourceId == null) {
            logger.warn(
                    "no 'from' attribute, and cannot uniquely determine sending resource for initiating entity {} in session {}",
                    initiatingEntity.getFullQualifiedName(), sessionContext.getSessionId());
            return null;
        }

        return new EntityImpl(initiatingEntity, resourceId);
    }
}
