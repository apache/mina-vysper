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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.ReturnErrorToSenderFailureStrategy;

/**
 * handling message stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MessageHandler extends XMPPCoreStanzaHandler {
    public String getName() {
        return "message";
    }

    @Override
    protected boolean verifyType(Stanza stanza) {
        return MessageStanza.isOfType(stanza);
    }


    @Override
    protected Stanza executeCore(XMPPCoreStanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza, SessionContext sessionContext) {

        // (try to) read thread id
        String threadId = null;
        XMLElement threadElement = null;
        try {
            threadElement = stanza.getSingleInnerElementsNamed("thread");
            if (threadElement != null && threadElement.getSingleInnerText() != null) {
                try {
                    threadId = threadElement.getSingleInnerText().getText();
                } catch (Exception _) {
                    threadId = null;
                }
            }
        } catch (XMLSemanticError _) {
            threadId = null;
        }

        // (try to) read subject id
        String subject = null;
        XMLElement subjectElement = null;
        try {
            subjectElement = stanza.getSingleInnerElementsNamed("subject");
            if (subjectElement != null && subjectElement.getSingleInnerText() != null) {
                try {
                    subject = subjectElement.getSingleInnerText().getText();
                } catch (Exception _) {
                    subject = null;
                }
            }
        } catch (XMLSemanticError _) {
            subject = null;
        }

        // TODO inspect all BODY elements and make sure they conform to the spec

        if (isOutboundStanza) {
            // check if message reception is turned of either globally or locally
            if (!serverRuntimeContext.getServerFeatures().isRelayingMessages() ||
                (sessionContext != null && sessionContext.getAttribute(SessionContext.SESSION_ATTRIBUTE_MESSAGE_STANZA_NO_RECEIVE) != null)) {
                return null;
            }

            Entity from = stanza.getFrom();
            if (from == null || !from.isResourceSet()) {
                // rewrite stanza with new from
                from = new EntityImpl(sessionContext.getInitiatingEntity(), serverRuntimeContext.getResourceRegistry().getUniqueResourceForSession(sessionContext));
                StanzaBuilder stanzaBuilder = new StanzaBuilder(stanza.getName());
                for (Attribute attribute : stanza.getAttributes()) {
                    if ("from".equals(attribute.getName())) continue;
                    stanzaBuilder.addAttribute(attribute);
                }
                stanzaBuilder.addAttribute(new Attribute("from", from.getFullQualifiedName()));
                for (XMLElement preparedElement : stanza.getInnerElements()) {
                    stanzaBuilder.addPreparedElement(preparedElement);
                }
                stanza = XMPPCoreStanza.getWrapper(stanzaBuilder.getFinalStanza());
            }

            StanzaRelay stanzaRelay = serverRuntimeContext.getStanzaRelay();
            try {
                stanzaRelay.relay(stanza.getTo(), stanza, new ReturnErrorToSenderFailureStrategy(stanzaRelay));
            } catch (Exception e) {
                // TODO return error stanza
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else if (sessionContext != null) {
            sessionContext.getResponseWriter().write(stanza);
        } else {
            throw new IllegalStateException("handling offline messages not implemented");
        }
        return null;
    }
}
