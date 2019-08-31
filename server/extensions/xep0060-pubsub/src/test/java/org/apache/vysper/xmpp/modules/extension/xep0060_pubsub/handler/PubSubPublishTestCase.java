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
import org.apache.vysper.xmpp.state.resourcebinding.BindException;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubSubPublishTestCase extends AbstractPublishSubscribeTestCase {
    protected LeafNode node = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        node = new LeafNode(serviceConfiguration, "news", "Node used for testing purposes", client);
        root.add(node);
    }

    @Override
    protected AbstractStanzaGenerator getDefaultStanzaGenerator() {
        return new DefaultPublishStanzaGenerator();
    }

    @Override
    protected IQHandler getHandler() {
        return new PubSubPublishHandler(serviceConfiguration);
    }

    public void testPublishResponse() {
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();

        node.subscribe("id", client);
        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id123", "news"), true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.RESULT.value(), response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        // get the subscription Element
        XMLElement pub = response.getFirstInnerElement().getFirstInnerElement();
        XMLElement item = pub.getFirstInnerElement();

        assertEquals("publish", pub.getName());
        assertEquals("news", pub.getAttributeValue("node"));
        assertNotNull(item); // should be present
        assertNotNull(item.getAttributeValue("id"));
    }

    public void testPublishWithSubscriber() throws BindException {
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();

        // create two subscriber for the node
        Entity francisco = createUser("francisco@denmark.lit");
        Entity bernardo = createUser("bernardo@denmark.lit/somewhere");

        // subscribe them
        node.subscribe("franid", francisco);
        node.subscribe("bernid", bernardo);

        // subscribe the publisher
        node.subscribe("id", client);

        assertEquals(3, node.countSubscriptions());

        // publish a message
        Stanza result = sendStanza(sg.getStanza(client, pubsubService, "id1", "news"), true);

        // verify response
        assertNotNull(result);

        IQStanza response = new IQStanza(result);

        assertEquals(IQStanzaType.RESULT.value(), response.getType());

        assertEquals("id1", response.getAttributeValue("id")); // IDs must match

        // get the query Element
        XMLElement pubsub = response.getFirstInnerElement();
        XMLElement publish = pubsub.getFirstInnerElement();
        XMLElement item = publish.getFirstInnerElement();

        assertEquals("pubsub", pubsub.getName());
        assertEquals(NamespaceURIs.XEP0060_PUBSUB, pubsub.getNamespaceURI());
        assertEquals("publish", publish.getName());
        assertEquals("item", item.getName());
        assertNotNull(item.getAttributeValue("id")); // value unknown

        // verify that each subscriber received the message
        assertEquals(3, relay.getCountRelayed()); // three subscribers
    }

    public void testPublishNoSuchNode() throws Exception {
        DefaultPublishStanzaGenerator sg = new DefaultPublishStanzaGenerator();
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

    public void testPublishForbidden() throws Exception {
        DefaultPublishStanzaGenerator sg = new DefaultPublishStanzaGenerator();
        Entity yodaNotSubscribed = new EntityImpl("yoda", "vysper.org", "dagobah"); // yoda@vysper.org/dagobah

        Stanza result = sendStanza(sg.getStanza(yodaNotSubscribed, pubsubService, "id123", "news"), true);
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

    protected Entity createUser(String jid) throws BindException {
        String boundResourceId = sessionContext.bindResource();
        Entity usr = new EntityImpl(clientBare, boundResourceId);
        return usr;
    }

    class DefaultPublishStanzaGenerator extends AbstractStanzaGenerator {
        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb, String node) {
            sb.startInnerElement("publish", NamespaceURIs.XEP0060_PUBSUB);
            sb.addAttribute("node", node);
            sb.startInnerElement("item", NamespaceURIs.XEP0060_PUBSUB);
            sb.addText("this is a test");
            sb.endInnerElement();
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
