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
package org.apache.vysper.xmpp.modules.core.bind.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;

import java.util.Collections;
import java.util.List;

/**
 * handles bind requests
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BindIQHandler extends DefaultIQHandler {

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND) && 
            verifyInnerElementWorker(stanza, "bind");
    }

    @Override
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {

        // As per RFC3920.7, the client may propose a resource id to the server:
        //
        // <iq type='set' id='bind_2'>
        // <bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>
        //   <resource>someresource</resource>
        // </bind>
        // </iq>
        //
        // The client's proposed resource id is ignored by this server.

        String resourceId = null;
        try {
            resourceId = sessionContext.bindResource();
        } catch (BindException e) {
            return bindError(stanza, sessionContext);
        }

        Entity entity = new EntityImpl(sessionContext.getInitiatingEntity(), resourceId);

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(null, null, IQStanzaType.RESULT, stanza.getID())
                .startInnerElement("bind", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND).startInnerElement("jid",
                        NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND).addText(entity.getFullQualifiedName())
                .endInnerElement().endInnerElement();

        return Collections.singletonList(stanzaBuilder.build());
    }

    private List<Stanza> bindError(IQStanza stanza, SessionContext sessionContext) {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(null, null, IQStanzaType.ERROR, stanza.getID())
                .startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "cancel")
                .startInnerElement("not-allowed", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();

        return Collections.singletonList(stanzaBuilder.build());
    }

}
