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

import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoInfoIQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractServerInfoDiscoTestCase extends AbstractDiscoTestCase {

    protected ServerInfoRequestListener getServerInfoRequestListener() {
        Module module = getModule();
        if(module instanceof ServerInfoRequestListener) {
            return (ServerInfoRequestListener) module;
        } else {
            throw new RuntimeException("Module does not implement ServerInfoRequestListener");
        }
    }
    
    /**
     * Default, expect no identity
     */
    protected Identity getExpectedIdentity() {
        return null;
    }

    /**
     * Default, expect no features
     */
    protected List<String> getExpectedServerInfoFeatures() {
        return Collections.emptyList();
    }
    
    public void testDiscoServerInfo() throws Exception {
        ServiceCollector serviceCollector = new ServiceCollector();
        serviceCollector.addServerInfoRequestListener(getServerInfoRequestListener());

        DefaultServerRuntimeContext runtimeContext = new DefaultServerRuntimeContext(EntityImpl.parse("vysper.org"), null);
        runtimeContext.registerServerRuntimeContextService(serviceCollector);

        DiscoInfoIQHandler infoIQHandler = new DiscoInfoIQHandler();

        StanzaBuilder request = StanzaBuilder.createIQStanza(EntityImpl.parse("user@vysper.org"), EntityImpl.parse("vysper.org"), IQStanzaType.GET, "1");
        request.startInnerElement("query", "http://jabber.org/protocol/disco#info").endInnerElement();
        
        ResponseStanzaContainer resultStanzaContainer = infoIQHandler.execute(request.getFinalStanza(), runtimeContext, false, new TestSessionContext(runtimeContext, new SessionStateHolder()), null);
        Stanza resultStanza = resultStanzaContainer.getResponseStanza();

        XMLElement queryElement = resultStanza.getFirstInnerElement();
        
        assertIdentity(queryElement);
        
        assertFeatures(queryElement);
    }

    private void assertIdentity(XMLElement queryElement)
            throws XMLSemanticError {
        Identity expectedIdentity = getExpectedIdentity();
        if(expectedIdentity != null) {
            XMLElement identityElement = queryElement.getSingleInnerElementsNamed("identity");
            
            assertNotNull("Identity element must exist", identityElement);
            
            assertEquals("Identity category", expectedIdentity.getCategory(), identityElement.getAttributeValue("category"));
            assertEquals("Identity type", expectedIdentity.getType(), identityElement.getAttributeValue("type"));
            
            if(expectedIdentity.getName() != null) {
                assertEquals("Identity name", expectedIdentity.getName(), identityElement.getAttributeValue("name"));
            } else {
                assertNull("Identity name attribute should be missing", identityElement.getAttributeValue("name"));
            }
        }
    }

    private void assertFeatures(XMLElement queryElement) {
        List<XMLElement> featureElements = queryElement.getInnerElementsNamed("feature");
        List<String> expectedFeatures = new ArrayList<String>(getExpectedServerInfoFeatures());
        // order is random, check that all namespaces are present
        for(XMLElement element : featureElements) {
            expectedFeatures.remove(element.getAttributeValue("var"));
        }
        
        assertTrue("Disco response missing features: " + expectedFeatures.toString(), expectedFeatures.size() == 0);
    }
}
