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

import java.util.Collections;
import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * handling message stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultMessageHandler extends XMPPCoreStanzaHandler {

    public String getName() {
        return "message";
    }

    @Override
    protected boolean verifyType(Stanza stanza) {
        return MessageStanza.isOfType(stanza);
    }

    protected boolean verifyInnerNamespace(Stanza stanza, String namespace) {
        XMLElementVerifier xmlElementVerifier = stanza.getVerifier();
        if (!xmlElementVerifier.subElementsPresentAtLeast(1))
            return false;

        List<XMLElement> innerElements = stanza.getInnerElements();
        XMLElement firstInnerElement = innerElements.get(0);
        return firstInnerElement.getVerifier().namespacePresent(namespace);
    }

    @Override
    protected List<Stanza> executeCore(XMPPCoreStanza coreStanza, ServerRuntimeContext serverRuntimeContext,
                                       boolean isOutboundStanza, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        MessageStanza stanza = (MessageStanza) coreStanza;

        return executeMessageLogic(stanza, serverRuntimeContext, sessionContext, stanzaBroker);
    }

    /**
     * must be overridden by specialized message handlers
     */
    protected List<Stanza> executeMessageLogic(MessageStanza stanza, ServerRuntimeContext serverRuntimeContext,
											   SessionContext sessionContext, StanzaBroker stanzaBroker) {
        // this is default behavior and must be replaced by overrider
        return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED, stanza,
                StanzaErrorType.CANCEL, null, null, null));
    }

}
