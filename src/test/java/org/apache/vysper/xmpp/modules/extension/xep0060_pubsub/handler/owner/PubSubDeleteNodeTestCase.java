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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner;

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.AbstractStanzaGenerator;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubSubDeleteNodeTestCase extends AbstractPublishSubscribeTestCase {
    
    class DefaultDeleteNodeStanzaGenerator extends AbstractStanzaGenerator {
        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb, String node) {
            sb.startInnerElement("delete");
            sb.addAttribute("node", node);
            sb.endInnerElement();
            return sb;
        }

        @Override
        protected String getNamespace() {
            return NamespaceURIs.XEP0060_PUBSUB_OWNER;
        }

        @Override
        protected IQStanzaType getStanzaType() {
            return IQStanzaType.SET;
        }
    }

    @Override
    protected AbstractStanzaGenerator getDefaultStanzaGenerator() {
        return new DefaultDeleteNodeStanzaGenerator();
    }

    @Override
    protected IQHandler getHandler() {
        return new PubSubOwnerDeleteNodeHandler(root);
    }

    public void testDelete() throws Exception {
        String testNode = "test";
        LeafNode node = root.createNode(pubsubService, testNode);
        node.subscribe("someid", client); // make the client subscriber (=owner)
        node.subscribe("otherid1", new EntityImpl("yoda", "starwars.com", "spaceship"));
        node.subscribe("otherid2", new EntityImpl("r2d2", "starwars.com", "desert"));
        node.subscribe("otherid3", new EntityImpl("anakin", "starwars.com", "deathstar"));
        
        assertNotNull(root.find(testNode));
        // make sure we have 4 subscribers
        assertEquals(4, node.countSubscriptions());
        
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService, "id123", testNode);
        ResponseStanzaContainer result = sendStanza(stanza, true);
        assertTrue(result.hasResponse());
        IQStanza response = new IQStanza(result.getResponseStanza());
        assertEquals(IQStanzaType.RESULT.value(),response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match
        
        LeafNode n = root.find(testNode);
        assertNull(n);
        
        // check that the subscribers got a notification
        assertEquals(4, relay.getCountRelayed());
    }
    
    public void testDeleteNotAuth() throws Exception {
        String testNode = "test";
        root.createNode(pubsubService, testNode);
        
        assertNotNull(root.find(testNode));
        
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService, "id123", testNode);
        ResponseStanzaContainer result = sendStanza(stanza, true);
        assertTrue(result.hasResponse());
        IQStanza response = new IQStanza(result.getResponseStanza());
        assertEquals(IQStanzaType.ERROR.value(),response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match
        
        XMLElement error = response.getInnerElementsNamed("error").get(0); //jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("auth", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(1, errorContent.size());
        assertEquals("forbidden", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespace());
        
        LeafNode n = root.find(testNode);
        assertNotNull(n); // still there
    }
    
    public void testDeleteNoSuchNode() throws Exception {
        String testNode = "test";
        assertNull(root.find(testNode));
        
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService, "id123", testNode);
        ResponseStanzaContainer result = sendStanza(stanza, true);
        assertTrue(result.hasResponse());
        IQStanza response = new IQStanza(result.getResponseStanza());
        assertEquals(IQStanzaType.ERROR.value(),response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match
        
        XMLElement error = response.getInnerElementsNamed("error").get(0); //jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("cancel", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(1, errorContent.size());
        assertEquals("item-not-found", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespace());
    }
}
