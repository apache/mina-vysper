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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverQueue;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.TestSessionContext;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.MucUserPresenceItem;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Status.StatusCode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public abstract class AbstractMUCHandlerTestCase extends TestCase {
    
    protected TestSessionContext sessionContext;

    protected static final String SERVERDOMAIN = "test";
    protected static final String SUBDOMAIN = "chat";
    protected static final String FULLDOMAIN = SUBDOMAIN + "." + SERVERDOMAIN;
    
    protected static final Entity MODULE_JID = EntityImpl.parseUnchecked(FULLDOMAIN);

    protected static final Entity ROOM1_JID = EntityImpl.parseUnchecked("room1@" + FULLDOMAIN);
    protected static final Entity ROOM2_JID = EntityImpl.parseUnchecked("room2@" + FULLDOMAIN);

    protected static final Entity ROOM1_JID_WITH_NICK = EntityImpl.parseUnchecked("room1@" + FULLDOMAIN + "/nick");
    protected static final Entity ROOM2_JID_WITH_NICK = EntityImpl.parseUnchecked("room2@" + FULLDOMAIN + "/nick");
    
    protected static final Entity OCCUPANT1_JID = EntityImpl.parseUnchecked("user1@" + SERVERDOMAIN);
    protected static final Entity OCCUPANT2_JID = EntityImpl.parseUnchecked("user2@" + SERVERDOMAIN);
    protected StanzaHandler handler;

    protected Conference conference = new Conference("foo");

    protected StanzaReceiverQueue occupant1Queue = new StanzaReceiverQueue();

    protected StanzaReceiverQueue occupant2Queue = new StanzaReceiverQueue();
    
    @Override
    protected void setUp() throws Exception {
        sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
        sessionContext.setInitiatingEntity(OCCUPANT1_JID);
        
        StanzaReceiverRelay stanzaRelay = (StanzaReceiverRelay) sessionContext.getServerRuntimeContext().getStanzaRelay();
        stanzaRelay.add(OCCUPANT1_JID, occupant1Queue);
        stanzaRelay.add(OCCUPANT2_JID, occupant2Queue);
        
        conference.createRoom(ROOM1_JID, "Room 1");
        
        handler = createHandler();
    }
    
    protected abstract StanzaHandler createHandler();
    
    
    protected void assertErrorStanza(Stanza response, String stanzaName, Entity from, Entity to, 
            String type, String errorName, XMLElement... expectedInnerElements) {
        assertNotNull(response);
        assertEquals(stanzaName, response.getName());
        assertEquals(to, response.getTo());
        assertEquals(from, response.getFrom());
        assertEquals("error", response.getAttributeValue("type"));
        
        List<XMLElement> innerElements = response.getInnerElements();

        int index = 0;
        if(expectedInnerElements != null) {
            for(XMLElement expectedInnerElement : expectedInnerElements) {
                assertEquals(new Renderer(expectedInnerElement).getComplete() + "\n" + new Renderer(innerElements.get(index)).getComplete(), expectedInnerElement, innerElements.get(index));
                index++;
            }
        }
        
        // error element must always be present
        XMLElement errorElement = innerElements.get(index);
        assertEquals("error", errorElement.getName());
        assertEquals(type, errorElement.getAttributeValue("type"));
        
        XMLElement jidMalformedElement = errorElement.getFirstInnerElement();
        assertEquals(errorName, jidMalformedElement.getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, jidMalformedElement.getNamespaceURI());
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
            assertEquals(type, msgStanza.getType());
        }

        assertEquals(expectedBody, msgStanza.getBody(null));
        assertEquals(expectedSubject, msgStanza.getSubject(null));

        if(expectedX != null) {
            X actualX = X.fromStanza(stanza);
            assertEquals(expectedX, actualX);
        }
    }

    protected void assertIqResultStanza(Entity from, Entity to, String id, 
            Stanza stanza) throws XMLSemanticError {
        assertNotNull(stanza);
        IQStanza iqStanza = (IQStanza) IQStanza.getWrapper(stanza);
        
        assertEquals(from, iqStanza.getFrom());
        assertEquals(to, iqStanza.getTo());
        assertEquals(id, iqStanza.getID());
        assertEquals("result", iqStanza.getType());
    }
    
    protected void assertPresenceStanza(Stanza stanza, Entity expectedFrom, Entity expectedTo, String expectedShow,
            String expectedStatus,
            MucUserPresenceItem expectedItem) throws XMLSemanticError, Exception {

        PresenceStanza presenceStanza = (PresenceStanza) PresenceStanza.getWrapper(stanza);
        assertNotNull("Stanza must not be null", stanza);
        assertEquals(expectedFrom, stanza.getFrom());
        assertEquals(expectedTo, stanza.getTo());
        assertEquals(expectedShow, presenceStanza.getShow());
        assertEquals(expectedStatus, presenceStanza.getStatus(null));
        
        XMLElement xElm = stanza.getSingleInnerElementsNamed("x");
        assertEquals(NamespaceURIs.XEP0045_MUC_USER, xElm.getNamespaceURI());
        
        List<XMLElement> innerElements = xElm.getInnerElements();
            
        assertEquals(1, innerElements.size());
        XMLElement itemElm = innerElements.get(0);
        assertEquals("item", itemElm.getName());
        assertEquals(expectedItem.getJid().getFullQualifiedName(), itemElm.getAttributeValue("jid"));
        assertEquals(expectedItem.getNick(), itemElm.getAttributeValue("nick"));
        assertEquals(expectedItem.getAffiliation().toString(), itemElm.getAttributeValue("affiliation"));
        assertEquals(expectedItem.getRole().toString(), itemElm.getAttributeValue("role"));
        
    }

    protected void assertPresenceStanza(Stanza stanza, Entity expectedFrom, Entity expectedTo, String expectedType,
            MucUserPresenceItem expectedItem, StatusCode expectedStatus) throws Exception {
    	List<MucUserPresenceItem> expectedItems = Arrays.asList(expectedItem);
    	List<StatusCode> expectedStatuses = Arrays.asList(expectedStatus);
    	assertPresenceStanza(stanza, expectedFrom, expectedTo, expectedType, expectedItems, expectedStatuses);
    }
    
    protected void assertPresenceStanza(Stanza stanza, Entity expectedFrom, Entity expectedTo, String expectedType,
            List<MucUserPresenceItem> expectedItems, List<StatusCode> expectedStatuses) throws Exception {

        assertNotNull(stanza);
        assertEquals(expectedFrom, stanza.getFrom());
        assertEquals(expectedTo, stanza.getTo());
        assertEquals(expectedType, stanza.getAttributeValue("type"));
        
        XMLElement xElm = stanza.getFirstInnerElement();
        assertEquals(NamespaceURIs.XEP0045_MUC_USER, xElm.getNamespaceURI());
        
        Iterator<XMLElement> innerElements = xElm.getInnerElements().iterator();
        for(MucUserPresenceItem expectedItem : expectedItems) {
            XMLElement itemElm = innerElements.next();
            
            assertEquals("item", itemElm.getName());
            if(expectedItem.getJid() != null) {
            	assertEquals(expectedItem.getJid().getFullQualifiedName(), itemElm.getAttributeValue("jid"));
            } else {
            	assertNull(itemElm.getAttributeValue("jid"));
            }
            assertEquals(expectedItem.getNick(), itemElm.getAttributeValue("nick"));
            assertEquals(expectedItem.getAffiliation().toString(), itemElm.getAttributeValue("affiliation"));
            assertEquals(expectedItem.getRole().toString(), itemElm.getAttributeValue("role"));
        }
        
        if(expectedStatuses != null) {
            for(StatusCode status : expectedStatuses) {
                XMLElement statusElm = innerElements.next();
    
                assertEquals("status", statusElm.getName());
                assertEquals(status.code(), Integer.parseInt(statusElm.getAttributeValue("code")));
    
            }
        }
    }
    
    protected Stanza sendIq(Entity from, Entity to, IQStanzaType type, String id, String namespaceUri, 
    		XMLElement item) throws ProtocolException {
    	StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(from, to, type, id);

    	stanzaBuilder.startInnerElement("query", namespaceUri);
    	stanzaBuilder.addPreparedElement(item);
    	stanzaBuilder.endInnerElement();
    	
        Stanza iqStanza = stanzaBuilder.build();
        ResponseStanzaContainer container = handler.execute(iqStanza,
                sessionContext.getServerRuntimeContext(), true, sessionContext,
                null);
        if (container != null) {
            return container.getResponseStanza();
        } else {
            return null;
        }
    }



}
