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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubAffiliation;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.AbstractStanzaGenerator;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubSubOwnerRetrieveAffiliationsTestCase extends AbstractPublishSubscribeTestCase {
    protected LeafNode n1 = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Entity client2 = new EntityImpl("user1", "vysper.org", null);
        Entity client3 = new EntityImpl("user2", "vysper.org", null);

        n1 = new LeafNode(serviceConfiguration, "Node1", "Node 1 used for testing purposes", client);
        n1.setAffiliation(client2, PubSubAffiliation.OWNER);
        n1.setAffiliation(client3, PubSubAffiliation.OWNER);

        root.add(n1);
    }

    @Override
    protected IQHandler getHandler() {
        return new PubSubOwnerManageAffiliationsHandler(serviceConfiguration);
    }

    @Override
    protected AbstractStanzaGenerator getDefaultStanzaGenerator() {
        return new DefaultRetrieveAffiliationsStanzaGenerator("Node1");
    }

    public void testRetrieveAffiliationsNoAuth() {
        Entity client2 = new EntityImpl("yoda", "starwars.com", null);

        AbstractStanzaGenerator sg = new DefaultRetrieveAffiliationsStanzaGenerator("Node1");
        Stanza stanza = sg.getStanza(client2, pubsubService, "id123", null);
        Stanza result = sendStanza(stanza, true);

        assertNotNull(result);

        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("auth", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(1, errorContent.size());
        assertEquals("forbidden", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());
    }

    public void testRetrieveAffiliationsNoSuchNode() throws Exception {
        String testNode = "test";
        assertNull(root.find(testNode));

        AbstractStanzaGenerator sg = new DefaultRetrieveAffiliationsStanzaGenerator("test");
        Stanza stanza = sg.getStanza(client, pubsubService, "id123", testNode);
        Stanza result = sendStanza(stanza, true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElementsNamed("error").get(0); // jump directly to the error part
        assertEquals("error", error.getName());
        assertEquals("cancel", error.getAttributeValue("type"));

        List<XMLElement> errorContent = error.getInnerElements();
        assertEquals(1, errorContent.size());
        assertEquals("item-not-found", errorContent.get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, errorContent.get(0).getNamespaceURI());
    }

    public void testRetrieveAffiliations() {
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();

        Stanza stanza = sg.getStanza(client, pubsubService, "4711", null);
        Stanza result = sendStanza(stanza, true);

        assertNotNull(result);

        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.RESULT.value(), response.getType());
        XMLElement sub = response.getFirstInnerElement().getFirstInnerElement();
        assertEquals("affiliations", sub.getName());
        assertEquals(3, sub.getInnerElements().size());

        for (XMLElement e : sub.getInnerElements()) {
            assertEquals("owner", e.getAttributeValue("affiliation"));
        }
    }

    class DefaultRetrieveAffiliationsStanzaGenerator extends AbstractStanzaGenerator {
        private String node;

        public DefaultRetrieveAffiliationsStanzaGenerator(String nodeName) {
            this.node = nodeName;
        }

        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb, String node) {
            sb.startInnerElement("affiliations", NamespaceURIs.XEP0060_PUBSUB);
            sb.addAttribute("node", this.node);
            sb.endInnerElement();
            return sb;
        }

        @Override
        protected String getNamespace() {
            return NamespaceURIs.XEP0060_PUBSUB_OWNER;
        }

        @Override
        protected IQStanzaType getStanzaType() {
            return IQStanzaType.GET;
        }
    }

}