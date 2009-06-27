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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import junit.framework.TestCase;

import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.AbstractStanzaGenerator;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionnodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;
import org.apache.vysper.xmpp.writer.SystemOutStanzaWriter;


/**
 * The abstract base class for all pubsub related tests.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public abstract class AbstractPublishSubscribeTestCase extends TestCase {
    protected TestSessionContext sessionContext  = null;
    protected Entity clientBare = null;
    protected Entity client = null;
    protected Entity pubsub = null;
    protected IQHandler handler = null;
    protected CollectionNode root = null;
    protected LeafNode node = null;
    
    // for debugging
    protected SystemOutStanzaWriter stanzaWriter = new SystemOutStanzaWriter();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        sessionContext = createTestSessionContext();

        clientBare = new EntityImpl("tester", "vysper.org", null);
        sessionContext.setInitiatingEntity(clientBare);

        String boundResourceId = sessionContext.bindResource();
        client = new EntityImpl(clientBare, boundResourceId);
        pubsub = EntityImpl.parse("pubsub.vysper.org/news");
        setResourceConnected(boundResourceId);

        root = new CollectionNode();
        node = root.createNode(pubsub);

        handler = getHandler();
    }
    
    protected TestSessionContext createTestSessionContext() {
        SessionStateHolder sessionStateHolder = new SessionStateHolder();
        sessionStateHolder.setState(SessionState.AUTHENTICATED);
        StanzaReceiverRelay relay = new org.apache.vysper.xmpp.delivery.StanzaReceiverRelay();
        DefaultServerRuntimeContext serverContext = new DefaultServerRuntimeContext(new EntityImpl(null, "pubsub.vysper.org", null), relay);
        relay.setServerRuntimeContext(serverContext);
        TestSessionContext tsc = new TestSessionContext(serverContext, sessionStateHolder);

        configureStorageProvider(tsc);
        configureServiceRegistry(tsc);
        configurePubsubModule(tsc);
        return tsc;
    }

    protected void configureServiceRegistry(TestSessionContext tsc) {
        ServiceCollector serviceCollector = new ServiceCollector();
        ((DefaultServerRuntimeContext) tsc.getServerRuntimeContext()).registerServerRuntimeContextService(serviceCollector);
    }

    protected void configurePubsubModule(TestSessionContext tsc) {
        ((DefaultServerRuntimeContext) tsc.getServerRuntimeContext()).addModule(new PublishSubscribeModule());
    }
    
    protected void configureStorageProvider(TestSessionContext tsc) {
        OpenStorageProviderRegistry storageProviderRegistry = new OpenStorageProviderRegistry();
        storageProviderRegistry.add(new CollectionnodeInMemoryStorageProvider());
        storageProviderRegistry.add(new LeafNodeInMemoryStorageProvider());
        ((DefaultServerRuntimeContext) tsc.getServerRuntimeContext()).setStorageProviderRegistry(storageProviderRegistry);
    }

    private void setResourceConnected(String boundResourceId) {
        sessionContext.getServerRuntimeContext().getResourceRegistry().setResourceState(boundResourceId, ResourceState.CONNECTED);
    }

    /**
     * Override and provide the Handler to be tested. A new
     * handler will be created for each test.
     * 
     * @return the instantiated handler to be tested
     */
    protected abstract IQHandler getHandler();

    /**
     * Return the StanzaGenerator that build the "typical" request for the given type of request.
     * 
     * This will be used to test whether the handler correctly accepts these stanzas.
     * 
     * @return the default Stanza generator for the request type.
     */
    protected abstract AbstractStanzaGenerator getDefaultStanzaGenerator();

    protected ResponseStanzaContainer sendStanza(Stanza toSend, boolean isOutboundStanza) {
        return handler.execute(toSend, sessionContext.getServerRuntimeContext(), isOutboundStanza, sessionContext, null);
    }

    public void testSimpleStanza() {
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsub, "id1");

        assertTrue(handler.verify(stanza));
    }
}
