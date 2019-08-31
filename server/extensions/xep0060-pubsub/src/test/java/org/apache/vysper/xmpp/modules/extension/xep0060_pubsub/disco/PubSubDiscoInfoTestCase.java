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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.disco;

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.feature.PubsubFeatures;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.AbstractStanzaGenerator;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoInfoIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

public class PubSubDiscoInfoTestCase extends AbstractPublishSubscribeTestCase {

    @Override
    protected AbstractStanzaGenerator getDefaultStanzaGenerator() {
        return new DefaultDiscoInfoStanzaGenerator();
    }

    @Override
    protected IQHandler getHandler() {
        return new DiscoInfoIQHandler();
    }

    public void testIdentityAndFeature() {
        DefaultDiscoInfoStanzaGenerator sg = (DefaultDiscoInfoStanzaGenerator) getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService.getBareJID(), "id123");

        Stanza result = sendStanza(stanza, true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);

        assertEquals(IQStanzaType.RESULT.value(), response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        // get the query Element
        XMLElement query = response.getFirstInnerElement();
        List<XMLElement> inner = query.getInnerElements();

        assertEquals("query", query.getName());

        // at least we have an identity and a feature element
        assertTrue(inner.size() >= 2);

        // ordering etc. is unknown; step through all subelements and pick the ones we
        // need
        XMLElement identity = null;
        XMLElement feature = null;
        for (XMLElement el : inner) {
            if (el.getName().equals("identity")
                    // && el.getNamespace().equals(NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO) //
                    // TODO enable when the parser is fixed
                    && el.getAttributeValue("category").equals("pubsub")
                    && el.getAttributeValue("type").equals("service")) {
                identity = el;
            } else if (el.getName().equals("feature")
                    /* && el.getNamespace().equals(NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO) */// TODO enable when
                                                                                                   // the parser is
                                                                                                   // fixed
                    && el.getAttributeValue("var").equals(NamespaceURIs.XEP0060_PUBSUB)) {
                feature = el;
            }
        }

        // make sure they were there (booleans would have sufficed)
        assertNotNull(identity);
        assertNotNull(feature);
    }

    public void testInfoRequestForANode() throws Exception {
        root.add(new LeafNode(serviceConfiguration, "news", "News", client));

        DefaultDiscoInfoStanzaGenerator sg = (DefaultDiscoInfoStanzaGenerator) getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService.getBareJID(), "id123", "news");

        Stanza result = sendStanza(stanza, true);
        assertNotNull(result);
        IQStanza response = new IQStanza(result);

        assertEquals(IQStanzaType.RESULT.value(), response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        // get the query Element
        XMLElement query = response.getFirstInnerElement();
        List<XMLElement> inner = query.getInnerElements();

        assertEquals("query", query.getName());

        // at least we have an identity element
        assertTrue(inner.size() >= 1);

        // ordering etc. is unknown; step through all subelements and pick the ones we
        // need
        XMLElement identity = null;
        for (XMLElement el : inner) {
            if (el.getName().equals("identity")
                    // && el.getNamespace().equals(NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO) //
                    // TODO enable when the parser is fixed
                    && el.getAttributeValue("category").equals("pubsub")
                    && el.getAttributeValue("type").equals("leaf")) {
                identity = el;
            }
        }

        // make sure they were there
        assertNotNull(identity);

        String[] featuresList = new String[] { NamespaceURIs.XEP0060_PUBSUB, PubsubFeatures.ACCESS_OPEN.toString(),
                PubsubFeatures.ITEM_IDS.toString(), PubsubFeatures.PERSISTENT_ITEMS.toString(),
                PubsubFeatures.MULTI_SUBSCRIBE.toString(), PubsubFeatures.PUBLISH.toString(),
                PubsubFeatures.SUBSCRIBE.toString(), PubsubFeatures.RETRIEVE_SUBSCRIPTIONS.toString(),
                PubsubFeatures.RETRIEVE_AFFILIATIONS.toString(), PubsubFeatures.MODIFY_AFFILIATIONS.toString() };
        XMLElement[] elementList = collectFeatures(inner, featuresList);

        for (int idx = 0; idx < elementList.length; ++idx) {
            assertNotNull(featuresList[idx], elementList[idx]); // add a more descriptive error message if the test
                                                                // fails
        }
    }

    private XMLElement[] collectFeatures(List<XMLElement> inner, String[] features) {
        XMLElement[] elementList = new XMLElement[features.length];
        Arrays.sort(features);
        for (XMLElement el : inner) {
            if (el.getName().equals("feature"))
            /* && el.getNamespace().equals(NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO) */ { // TODO enable when the
                                                                                              // parser is fixed
                int index = Arrays.binarySearch(features, el.getAttributeValue("var"));
                if (index != -1) {
                    elementList[index] = el;
                }
            }
        }
        return elementList;
    }

    class DefaultDiscoInfoStanzaGenerator extends AbstractStanzaGenerator {
        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb, String node) {
            return sb;
        }

        @Override
        protected String getNamespace() {
            return NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO;
        }

        @Override
        protected IQStanzaType getStanzaType() {
            return IQStanzaType.GET;
        }

        public Stanza getStanza(Entity client, Entity pubsub, String id) {
            StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(client, pubsub, getStanzaType(), id);
            stanzaBuilder.startInnerElement("query", getNamespace());

            return stanzaBuilder.build();
        }

        @Override
        public Stanza getStanza(Entity client, Entity pubsub, String id, String node) {
            StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(client, pubsub, getStanzaType(), id);
            stanzaBuilder.startInnerElement("query", getNamespace());
            stanzaBuilder.addAttribute("node", node);

            stanzaBuilder.endInnerElement();

            return stanzaBuilder.build();
        }
    }
}
