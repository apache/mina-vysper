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

import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoItemIQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractItemsDiscoTestCase extends AbstractDiscoTestCase {

    protected ItemRequestListener getItemRequestListener() {
        Module module = getModule();
        if(module instanceof ItemRequestListener) {
            return (ItemRequestListener) module;
        } else {
            throw new RuntimeException("Module does not implement ItemRequestListener");
        }
    }
    
    /**
     * Default, expect no features
     * @throws EntityFormatException 
     */
    protected List<Item> getExpectedItems() throws Exception {
        return Collections.emptyList();
    }
    
    public void testDiscoItems() throws Exception {
        ServiceCollector serviceCollector = new ServiceCollector();
        serviceCollector.addItemRequestListener(getItemRequestListener());

        DefaultServerRuntimeContext runtimeContext = new DefaultServerRuntimeContext(EntityImpl.parse("vysper.org"), null);
        runtimeContext.registerServerRuntimeContextService(serviceCollector);

        DiscoItemIQHandler itemIQHandler = new DiscoItemIQHandler();

        StanzaBuilder request = StanzaBuilder.createIQStanza(EntityImpl.parse("user@vysper.org"), EntityImpl.parse("vysper.org"), IQStanzaType.GET, "1");
        request.startInnerElement("query", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS).endInnerElement();
        
        ResponseStanzaContainer resultStanzaContainer = itemIQHandler.execute(request.getFinalStanza(), runtimeContext, false, new TestSessionContext(runtimeContext, new SessionStateHolder()), null);
        Stanza resultStanza = resultStanzaContainer.getResponseStanza();

        XMLElement queryElement = resultStanza.getFirstInnerElement();
        
        assertItems(queryElement);
    }

    private void assertItems(XMLElement queryElement) throws Exception {
        List<XMLElement> itemElements = queryElement.getInnerElementsNamed("item");
        List<Item> expectedItems = new ArrayList<Item>(getExpectedItems());
        // order is random, check that all namespaces are present
        for(Item item : expectedItems) {
            String expectedJID = item.getJid().getFullQualifiedName();

            boolean found = false;
            for(XMLElement element : itemElements) {
                String actualJID = element.getAttributeValue("jid");
                
                if(expectedJID.equals(actualJID)) {
                    assertItem(item, element);
                    found = true;
                    break;
                }
            }
            if(!found) {
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
        
        if(expectedName != null) {
            assertEquals("Name for item with JID: " + expectedJID, expectedName, actualName);
        } else {
            assertNull("Name must be null in item with JID: " + expectedJID, actualName);            
        }

        if(expectedNode != null) {
            assertEquals("Node for item with JID: " + expectedJID, expectedNode, actualNode);
        } else {
            assertNull("Node must be null in item with JID: " + expectedJID, actualNode);            
        }
    }
}
