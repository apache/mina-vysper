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
package org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.parser.XMLParserUtil;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0054", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class VcardTempIQHandler extends DefaultIQHandler {

    protected boolean returnEmptyVCardWhenNonExistent = true;

    protected VcardTempPersistenceManager persistenceManager;

    public void setPersistenceManager(VcardTempPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "vCard") && verifyInnerNamespace(stanza, NamespaceURIs.VCARD_TEMP);
    }

    @Override
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity to = stanza.getTo();
        Entity from = stanza.getFrom();
        if (from == null) {
            from = sessionContext.getInitiatingEntity();
        }

        // Not null, and not addressed to itself
        if (to != null && !to.getBareJID().equals(sessionContext.getInitiatingEntity().getBareJID())) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.FORBIDDEN, stanza,
                    StanzaErrorType.AUTH, "VCard only modifiable by the owner", null, null));
        }
        
        XMLElement vCardElement = null;
        try {
            vCardElement = stanza.getSingleInnerElementsNamed("vCard");
        } catch (XMLSemanticError xmlSemanticError) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                    StanzaErrorType.MODIFY, "vCard element is missing", null, null));
        }
        if(vCardElement == null) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                    StanzaErrorType.MODIFY, "vCard element is missing", null, null));            
        }
        
        String vcardContent = new Renderer(vCardElement).getComplete();

        if (persistenceManager == null) {
            return buildInteralStorageError(stanza);
        }

        boolean success = persistenceManager.setVcard(from, vcardContent);

        if (success) {
            return Collections.singletonList(StanzaBuilder.createIQStanza(null, from, IQStanzaType.RESULT, stanza.getID()).build());
        } else {
            return Collections.singletonList(StanzaBuilder.createIQStanza(null, from, IQStanzaType.ERROR, stanza.getID()).build());
        }
    }

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {

        Entity to = stanza.getTo();
        Entity from = stanza.getFrom();

        if (from == null) {
            from = sessionContext.getInitiatingEntity();
        }

        Entity requestedCard = to != null ? to.getBareJID() : from.getBareJID(); // no from? return own vcard

        if (persistenceManager == null) {
            return buildInteralStorageError(stanza);
        }
        String vcardXml = persistenceManager.getVcard(requestedCard);

        // from XEP-0054 3.1:
        // If no vCard exists, the server MUST return a stanza error (which SHOULD be <item-not-found/>)
        // or an IQ-result containing an empty <vCard/> element.
        if (vcardXml == null) {
            IQStanzaType iqStanzaType = returnEmptyVCardWhenNonExistent ? IQStanzaType.RESULT : IQStanzaType.ERROR;
            StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), iqStanzaType,
                    stanza.getID());
            stanzaBuilder.startInnerElement("vCard", NamespaceURIs.VCARD_TEMP).endInnerElement();
            if (returnEmptyVCardWhenNonExistent) {
                // keep it like it is
            } else {
                stanzaBuilder.startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", "cancel")
                        .startInnerElement("item-not-found", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS)
                        .endInnerElement().endInnerElement();
            }
            return Collections.singletonList(stanzaBuilder.build());
        }

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(),
                IQStanzaType.RESULT, stanza.getID());
        try {
            XMLElement elm = XMLParserUtil.parseDocument(vcardXml);
            stanzaBuilder.addPreparedElement(elm);
        } catch (Exception e) {
            return buildInteralStorageError(stanza);
        }
        return Collections.singletonList(stanzaBuilder.build());
    }
    
    private List<Stanza> buildInteralStorageError(XMPPCoreStanza stanza) {
        return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR,
                stanza, StanzaErrorType.WAIT, "internal storage inaccessible", null, null));
    }

}