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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubSubSubscribeTestCase extends AbstractPublishSubscribeTestCase {
    protected LeafNode node = null;

    protected String testNode = "news";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        node = new LeafNode(serviceConfiguration, "news", "Node used for testing purposes", client);
        root.add(node);
    }

    @Override
    protected IQHandler getHandler() {
        return new PubSubSubscribeHandler(serviceConfiguration);
    }

    @Override
    protected AbstractStanzaGenerator getDefaultStanzaGenerator() {
        return new DefaultSubscribeStanzaGenerator();
    }

    public void testSubscribe() throws Exception {
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService, "id123", testNode);
        Stanza result = sendStanza(stanza, true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.RESULT.value(), response.getType());
        assertTrue(node.isSubscribed(client));

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        // get the subscription Element
        XMLElement sub = response.getFirstInnerElement().getFirstInnerElement();

        assertEquals("subscription", sub.getName());
        assertEquals(testNode, sub.getAttributeValue("node"));
        assertEquals(client.getFullQualifiedName(), sub.getAttributeValue("jid"));
        assertNotNull(sub.getAttributeValue("subid")); // it should be present - value unknown
        assertEquals("subscribed", sub.getAttributeValue("subscription"));
    }

    public void testSubscribeNoFrom() throws Exception {
        DefaultSubscribeStanzaGenerator sg = (DefaultSubscribeStanzaGenerator) getDefaultStanzaGenerator();
        sg.overrideSubscriberJID(client.getFullQualifiedName());

        Stanza stanza = sg.getStanza(null, pubsubService, "id123", testNode);
        Stanza result = sendStanza(stanza, true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.RESULT.value(), response.getType());
        assertTrue(node.isSubscribed(client));

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        // get the subscription Element
        XMLElement sub = response.getFirstInnerElement().getFirstInnerElement();

        assertEquals("subscription", sub.getName());
        assertEquals(testNode, sub.getAttributeValue("node"));
        assertEquals(client.getFullQualifiedName(), sub.getAttributeValue("jid"));
        assertNotNull(sub.getAttributeValue("subid")); // it should be present - value unknown
        assertEquals("subscribed", sub.getAttributeValue("subscription"));
    }

    public void testSubscribeNonMatchingJIDs() {
        DefaultSubscribeStanzaGenerator sg = new DefaultSubscribeStanzaGenerator();
        sg.overrideSubscriberJID("someone@quite.dif/ferent");

        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", testNode), true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());
        assertFalse(node.isSubscribed(client));

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("modify", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(2, errorContent.size());
        assertEquals("bad-request", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());

        assertEquals("invalid-jid", errorContent.get(1).getName());
        assertEquals(NamespaceURIs.XEP0060_PUBSUB_ERRORS, errorContent.get(1).getNamespaceURI());
    }

    public void testSubscribeJIDMalformed() {
        DefaultSubscribeStanzaGenerator sg = new DefaultSubscribeStanzaGenerator();
        sg.overrideSubscriberJID("@@");

        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", testNode), true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());
        assertFalse(node.isSubscribed(client));

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("modify", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(1, errorContent.size());
        assertEquals("jid-malformed", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());
    }

    public void testSubscribeNoSuchNode() throws Exception {
        DefaultSubscribeStanzaGenerator sg = new DefaultSubscribeStanzaGenerator();
        Entity pubsubWrongNode = EntityImpl.parse("pubsub.vysper.org");

        Stanza result = sendStanza(sg.getStanza(client, pubsubWrongNode, "id123", "doesnotexist"), true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());
        assertFalse(node.isSubscribed(client));

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("cancel", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(1, errorContent.size());
        assertEquals("item-not-found", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());
    }

    class DefaultSubscribeStanzaGenerator extends AbstractStanzaGenerator {
        private String subscriberJID = null;

        private String getSubscriberJID(Entity client) {
            if (subscriberJID == null) {
                return client.getFullQualifiedName();
            }
            return subscriberJID;
        }

        /**
         * Use this method to force a different subscriber JID.
         * 
         * @param jid
         */
        public void overrideSubscriberJID(String jid) {
            this.subscriberJID = jid;
        }

        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb, String node) {
            sb.startInnerElement("subscribe", NamespaceURIs.XEP0060_PUBSUB);
            sb.addAttribute("node", node);
            sb.addAttribute("jid", getSubscriberJID(client));
            sb.endInnerElement();
            return sb;
        }

        @Override
        protected String getNamespace() {
            return NamespaceURIs.XEP0060_PUBSUB;
        }

        @Override
        protected IQStanzaType getStanzaType() {
            return IQStanzaType.SET;
        }
    }

}
