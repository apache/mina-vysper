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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * handling presence stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultPresenceHandler extends XMPPCoreStanzaHandler {

    public String getName() {
        return "presence";
    }

    @Override
    protected boolean verifyType(Stanza stanza) {
        return PresenceStanza.isOfType(stanza);
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
    protected Stanza executeCore(XMPPCoreStanza coreStanza, ServerRuntimeContext serverRuntimeContext,
            boolean isOutboundStanza, SessionContext sessionContext) {
        PresenceStanza stanza = (PresenceStanza) coreStanza;

        return executePresenceLogic(stanza, serverRuntimeContext, sessionContext);
    }

    /**
     * must be overridden by specialized presence handlers
     */
    protected Stanza executePresenceLogic(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
        // this is default behavior and must be replaced by overrider
        return ServerErrorResponses.getStanzaError(StanzaErrorCondition.FEATURE_NOT_IMPLEMENTED, stanza,
                StanzaErrorType.CANCEL, null, null, null);
    }

}
