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
public class PubSubUnsubscribeTestCase extends AbstractPublishSubscribeTestCase {
    protected LeafNode node = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        node = new LeafNode(serviceConfiguration, "news", "Node used for testing purposes", client);
        root.add(node);
    }

    class DefaultUnsubscribeStanzaGenerator extends AbstractStanzaGenerator {
        private String subscriberJID = null;

        private String subID = null;

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

        public void setSubID(String subid) {
            this.subID = subid;
        }

        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb, String node) {
            sb.startInnerElement("unsubscribe", NamespaceURIs.XEP0060_PUBSUB);
            sb.addAttribute("node", node);
            sb.addAttribute("jid", getSubscriberJID(client));

            if (subID != null && subID.length() > 0) {
                sb.addAttribute("subid", subID);
            }

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

    @Override
    protected AbstractStanzaGenerator getDefaultStanzaGenerator() {
        return new DefaultUnsubscribeStanzaGenerator();
    }

    @Override
    protected IQHandler getHandler() {
        return new PubSubUnsubscribeHandler(serviceConfiguration);
    }

    public void testUnsubscribe() throws Exception {
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();

        // subscribe the client to the default node
        node.subscribe("somethingarbitrary", client);

        // make sure it is subscribed
        assertTrue(node.isSubscribed(client));

        // unsubscribe via XMPP
        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", "news"), true);

        // check subscription and response stanza
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.RESULT.value(), response.getType());
        assertFalse(node.isSubscribed(client));
    }

    public void testUnsubscribeMultipleNoSubID() {
        DefaultUnsubscribeStanzaGenerator sg = new DefaultUnsubscribeStanzaGenerator();

        // subscribe two times
        node.subscribe("subid1", client);
        node.subscribe("subid2", client);

        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", "news"), true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());
        assertTrue(node.isSubscribed(client));
        assertEquals(2, node.countSubscriptions(client));

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("modify", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(2, errorContent.size());
        assertEquals("bad-request", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());

        assertEquals("subid-required", errorContent.get(1).getName());
        assertEquals(NamespaceURIs.XEP0060_PUBSUB_ERRORS, errorContent.get(1).getNamespaceURI());
    }

    public void testUnsubscribeNoSuchSubscriber() {
        DefaultUnsubscribeStanzaGenerator sg = new DefaultUnsubscribeStanzaGenerator();

        assertFalse(node.isSubscribed(client));
        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", "news"), true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());
        assertFalse(node.isSubscribed(client));
        assertEquals(0, node.countSubscriptions(client));

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("cancel", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(2, errorContent.size());
        assertEquals("unexpected-request", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());

        assertEquals("not-subscribed", errorContent.get(1).getName());
        assertEquals(NamespaceURIs.XEP0060_PUBSUB_ERRORS, errorContent.get(1).getNamespaceURI());
    }

    public void testUnsubscribeForbidden() throws Exception {
        DefaultUnsubscribeStanzaGenerator sg = new DefaultUnsubscribeStanzaGenerator();
        String yoda = "this@somesubscriber.is/yoda";
        sg.overrideSubscriberJID(yoda);

        node.subscribe("subid1", EntityImpl.parse(yoda));

        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", "news"), true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());
        assertFalse(node.isSubscribed(client));
        assertEquals(0, node.countSubscriptions(client));

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("auth", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(1, errorContent.size());
        assertEquals("forbidden", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());
    }

    public void testUnsubscribeNoSuchNode() {
        DefaultUnsubscribeStanzaGenerator sg = new DefaultUnsubscribeStanzaGenerator();
        Entity pubsubWrongNode = new EntityImpl(null, "pubsub.vysper.org", null);

        Stanza result = sendStanza(sg.getStanza(client, pubsubWrongNode, "id123", "doesnotexsist"), true);
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

    public void testUnsubscribeBadSubID() {
        DefaultUnsubscribeStanzaGenerator sg = new DefaultUnsubscribeStanzaGenerator();
        sg.setSubID("doesnotexist");

        // subscribe two times
        node.subscribe("subid1", client);
        node.subscribe("subid2", client);

        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", "news"), true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());
        assertTrue(node.isSubscribed(client));
        assertEquals(2, node.countSubscriptions(client)); // still 2 subscriptions

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("modify", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(2, errorContent.size());
        assertEquals("not-acceptable", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());

        assertEquals("invalid-subid", errorContent.get(1).getName());
        assertEquals(NamespaceURIs.XEP0060_PUBSUB_ERRORS, errorContent.get(1).getNamespaceURI());
    }

    public void testUnsubscribeJIDMalformed() {
        DefaultUnsubscribeStanzaGenerator sg = new DefaultUnsubscribeStanzaGenerator();
        sg.overrideSubscriberJID("@@");

        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", "news"), true);
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
}
