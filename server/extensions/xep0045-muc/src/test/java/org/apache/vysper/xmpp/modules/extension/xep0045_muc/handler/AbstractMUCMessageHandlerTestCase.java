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

import static org.apache.vysper.xmpp.stanza.MessageStanzaType.GROUPCHAT;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public abstract class AbstractMUCMessageHandlerTestCase extends AbstractMUCHandlerTestCase {

    protected static final String BODY = "Body";

    protected Stanza sendMessage(Entity from, Entity to, MessageStanzaType type,
            String body) throws ProtocolException {
        return sendMessage(from, to, type, body, null, null);
    }
    
    protected Stanza sendMessage(Entity from, Entity to, MessageStanzaType type,
            String body, X x, String subject) throws ProtocolException {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createMessageStanza(from,
                to, type, null, body);
        if(subject != null) {
            stanzaBuilder.startInnerElement("subject").addText(subject).endInnerElement();
        }
        if(x != null) {
            stanzaBuilder.addPreparedElement(x);
        }

        Stanza messageStanza = stanzaBuilder.build();
        ResponseStanzaContainer container = handler.execute(messageStanza,
                sessionContext.getServerRuntimeContext(), true, sessionContext,
                null);
        if (container != null) {
            return container.getResponseStanza();
        } else {
            return null;
        }
    }

    protected void assertMessageErrorStanza(Stanza response, Entity from,
            Entity to, String type, String errorName,
            XMLElement... expectedInnerElements) {
        assertErrorStanza(response, "message", from, to, type, errorName,
                expectedInnerElements);
    }

    protected void testNotAllowedMessage(Room room, String expectedErrorName)
            throws Exception {
        String body = "Message body";

        // now, let user 2 exit room
        Stanza errorStanza = sendMessage(OCCUPANT1_JID, ROOM1_JID,
                GROUPCHAT, body);

        XMLElement expectedBody = new XMLElementBuilder("body").addText(body).build();
        assertMessageErrorStanza(errorStanza, ROOM1_JID, OCCUPANT1_JID, "modify",
                expectedErrorName,expectedBody);

        // no message should be relayed
        assertNull(occupant1Queue.getNext());
        assertNull(occupant2Queue.getNext());
    }

    protected void assertMessageStanza(Entity from, Entity to, String type,
            String body, Stanza stanza) throws XMLSemanticError {
        assertMessageStanza(from, to, type, body, null, null, stanza);
    }
    
    protected void assertMessageStanza(Entity from, Entity to, String type,
            String expectedBody, String expectedSubject, X expectedX, Stanza stanza) throws XMLSemanticError {
        assertNotNull(stanza);
        MessageStanza msgStanza = (MessageStanza) MessageStanza.getWrapper(stanza);
        
        assertEquals(from, stanza.getFrom());
        assertEquals(to, stanza.getTo());
        if (type != null) {
            assertEquals(type, stanza.getAttributeValue("type"));
        }

        assertEquals(expectedBody, msgStanza.getBody(null));
        assertEquals(expectedSubject, msgStanza.getSubject(null));

        if(expectedX != null) {
            X actualX = X.fromStanza(stanza);
            assertEquals(expectedX, actualX);
        }
    }

    @Override
    protected StanzaHandler createHandler() {
        return new MUCMessageHandler(conference, MODULE_JID);
    }
}
