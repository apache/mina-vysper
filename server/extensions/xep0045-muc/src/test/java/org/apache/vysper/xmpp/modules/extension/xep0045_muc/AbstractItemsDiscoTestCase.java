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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoItemIQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractItemsDiscoTestCase extends AbstractDiscoTestCase {

    @Override
    protected IQHandler createDiscoIQHandler() {
        return new DiscoItemIQHandler();
    }

    /**
     * Default, expect no features
     * @throws EntityFormatException 
     */
    protected List<Item> getExpectedItems() throws Exception {
        return Collections.emptyList();
    }

    @Override
    protected StanzaBuilder buildRequest() {
        StanzaBuilder request = StanzaBuilder.createIQStanza(USER_JID, getTo(), IQStanzaType.GET, "1");
        request.startInnerElement("query", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS).endInnerElement();
        return request;
    }

    @Override
    protected void assertResponse(XMLElement queryElement) throws Exception {
        assertItems(queryElement);
    }

    private void assertItems(XMLElement queryElement) throws Exception {
        List<XMLElement> itemElements = queryElement.getInnerElementsNamed("item");
        List<Item> expectedItems = new ArrayList<Item>(getExpectedItems());
        assertEquals(itemElements.size(), expectedItems.size());
        // order is random, check that all namespaces are present
        for (Item item : expectedItems) {
            String expectedJID = item.getJid().getFullQualifiedName();

            boolean found = false;
            for (XMLElement element : itemElements) {
                String actualJID = element.getAttributeValue("jid");

                if (expectedJID.equals(actualJID)) {
                    assertItem(item, element);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new AssertionFailedError("Item missing from response: " + item.getJid().getFullQualifiedName());
            }
        }
    }

    private void assertItem(Item expected, XMLElement actual) {
        // we already know the JID is equal
        String expectedJID = expected.getJid().getFullQualifiedName();

        String expectedName = expected.getName();
        String expectedNode = expected.getNode();

        String actualName = actual.getAttributeValue("name");
        String actualNode = actual.getAttributeValue("node");

        if (expectedName != null) {
            assertEquals("Name for item with JID: " + expectedJID, expectedName, actualName);
        } else {
            assertNull("Name must be null in item with JID: " + expectedJID, actualName);
        }

        if (expectedNode != null) {
            assertEquals("Node for item with JID: " + expectedJID, expectedNode, actualNode);
        } else {
            assertNull("Node must be null in item with JID: " + expectedJID, actualNode);
        }
    }
}
