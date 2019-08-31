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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
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
public class PubSubCreateNodeTestCase extends AbstractPublishSubscribeTestCase {

    class DefaultCreateNodeStanzaGenerator extends AbstractStanzaGenerator {
        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb, String node) {
            sb.startInnerElement("create", NamespaceURIs.XEP0060_PUBSUB);
            sb.addAttribute("node", node);
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
        return new DefaultCreateNodeStanzaGenerator();
    }

    @Override
    protected IQHandler getHandler() {
        return new PubSubCreateNodeHandler(serviceConfiguration);
    }

    public void testCreate() throws Exception {
        String testNode = "test";
        assertNull(root.find(testNode)); // shouldn't be there

        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService, "id123", testNode);
        Stanza result = sendStanza(stanza, true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.RESULT.value(), response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        LeafNode n = root.find(testNode);
        assertEquals(testNode, n.getName());
    }

    public void testCreateDuplicate() throws Exception {
        String testNode = "test";
        root.add(new LeafNode(serviceConfiguration, testNode, client));
        assertNotNull(root.find(testNode)); // should be there

        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService, "id123", testNode);
        Stanza result = sendStanza(stanza, true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);
        assertEquals(IQStanzaType.ERROR.value(), response.getType());
        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        XMLElement error = response.getInnerElements().get(1); // jump over the original request
        XMLElement conflict = error.getFirstInnerElement();

        assertEquals("error", error.getName());
        assertEquals("cancel", error.getAttributeValue("type"));

        assertEquals("conflict", conflict.getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, conflict.getNamespaceURI());
    }
}
