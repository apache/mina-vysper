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
package org.apache.vysper.xmpp.modules.servicediscovery.handler;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoDataForm;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;

import junit.framework.TestCase;

/**
 */
public class ExtendedDiscoInfoTestCase extends TestCase {

    public void testExtendedInfo() throws EntityFormatException {

        // test if the data form is correctly added to the disco info response

        ServiceCollector serviceCollector = new ServiceCollector();
        serviceCollector.addInfoRequestListener(new InfoRequestListener() {
            public List<InfoElement> getInfosFor(InfoRequest request) throws ServiceDiscoveryRequestException {

                DataForm form = new DataForm();
                form.setTitle("formtitle");
                form.setType(DataForm.Type.form);

                List<InfoElement> infoElements = new ArrayList<InfoElement>();
                infoElements.add(new Feature("TEST:NAMESPACE"));
                infoElements.add(new Identity("testCat", "testType"));
                infoElements.add(new InfoDataForm(form));
                return infoElements;
            }
        });

        DefaultServerRuntimeContext runtimeContext = new DefaultServerRuntimeContext(EntityImpl.parse("vysper.org"),
                mock(StanzaRelay.class));
        runtimeContext.registerServerRuntimeContextService(serviceCollector);

        DiscoInfoIQHandler infoIQHandler = new DiscoInfoIQHandler();

        StanzaBuilder request = StanzaBuilder.createIQStanza(EntityImpl.parse("user@vysper.org"),
                EntityImpl.parse("info@vysper.org"), IQStanzaType.GET, "1");

        IQStanza finalStanza = (IQStanza) XMPPCoreStanza.getWrapper(request.build());

        InternalSessionContext sessionContext = new TestSessionContext(runtimeContext, new SessionStateHolder(),
                runtimeContext.getStanzaRelay());

        List<Stanza> resultStanzas = infoIQHandler.handleGet(finalStanza, runtimeContext, sessionContext,
                new DefaultStanzaBroker(runtimeContext.getStanzaRelay(), sessionContext));

        assertTrue(resultStanzas.get(0).getVerifier().onlySubelementEquals("query",
                NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO));
        XMLElement queryElement = resultStanzas.get(0).getFirstInnerElement();
        XMLElementVerifier queryVerifier = queryElement.getVerifier();
        assertTrue(queryVerifier.subElementsPresentExact(4));
        List<XMLElement> innerElements = queryElement.getInnerElements();
        XMLElement xmlElement = innerElements.get(innerElements.size() - 1);
        XMLElementVerifier xmlElementVerifier = xmlElement.getVerifier();
        assertTrue(xmlElementVerifier.nameEquals("x"));
    }

}
