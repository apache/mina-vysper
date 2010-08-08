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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import static org.apache.vysper.xmpp.stanza.IQStanzaType.SET;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.IqAdminItem;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 */
public abstract class AbstractGrantRevokeTestCase extends AbstractMUCHandlerTestCase {

    protected void assertChangeNotAllowed(String nickToGrant, StanzaErrorCondition expectedError, Affiliation newAffiliation, Role newRole) throws ProtocolException {
        
        // send message to room
        IQStanza result = (IQStanza) IQStanza.getWrapper(sendIq(OCCUPANT1_JID, ROOM2_JID, SET, "id1",
                NamespaceURIs.XEP0045_MUC_ADMIN, new IqAdminItem(nickToGrant, null, newRole, newAffiliation)));

        XMLElementBuilder builder = new XMLElementBuilder("query", NamespaceURIs.XEP0045_MUC_ADMIN).startInnerElement(
                "item", NamespaceURIs.XEP0045_MUC_ADMIN).addAttribute("nick", nickToGrant);
        if(newAffiliation != null) {
            builder.addAttribute("affiliation", newAffiliation.toString());
        }
        if(newRole != null) {
            builder.addAttribute("role", newRole.toString());
        }
        builder.endInnerElement();

        XMLElement expectedInner = builder.build();
        
        assertErrorStanza(result, "iq", ROOM2_JID, OCCUPANT1_JID, StanzaErrorType.CANCEL, expectedError, expectedInner);
    }
    
    @Override
    protected StanzaHandler createHandler() {
        return new MUCIqAdminHandler(conference);
    }

}
