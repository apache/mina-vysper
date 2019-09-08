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

import org.apache.vysper.RecordingStanzaBroker;
import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.AbstractStanzaGenerator;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.DefaultStanzaBroker;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;
import org.apache.vysper.xmpp.writer.SystemOutStanzaWriter;

import junit.framework.TestCase;

/**
 * The abstract base class for all pubsub related tests.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public abstract class AbstractPublishSubscribeTestCase extends TestCase {
    protected TestSessionContext sessionContext = null;

    protected Entity clientBare = null;

    protected Entity client = null;

    protected Entity pubsubService = null;

    protected IQHandler handler = null;

    protected PubSubServiceConfiguration serviceConfiguration = null;

    protected CollectionNode root = null;

    protected Entity serverEntity = null;

    protected StanzaReceiverRelay relay = null;

    // for debugging
    private SystemOutStanzaWriter stanzaWriter = new SystemOutStanzaWriter();

    protected void printStanza(Stanza stanza) {
        this.stanzaWriter.write(stanza);
        this.stanzaWriter.close();
        System.out.println();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        serverEntity = new EntityImpl(null, "vysper.org", null);
        sessionContext = createTestSessionContext(serverEntity);

        root = new CollectionNode();
        serviceConfiguration = new PubSubServiceConfiguration(root);
        configurePubsubModule(sessionContext, serviceConfiguration);

        clientBare = new EntityImpl("tester", "vysper.org", null);
        sessionContext.setInitiatingEntity(clientBare);

        String boundResourceId = sessionContext.bindResource();
        client = new EntityImpl(clientBare, boundResourceId);
        pubsubService = EntityImpl.parse("pubsub.vysper.org");
        setResourceConnected(boundResourceId);

        handler = getHandler();
    }

    protected TestSessionContext createTestSessionContext(Entity serverEntity) {
        SessionStateHolder sessionStateHolder = new SessionStateHolder();
        sessionStateHolder.setState(SessionState.AUTHENTICATED);
        relay = new org.apache.vysper.xmpp.delivery.StanzaReceiverRelay();
        DefaultServerRuntimeContext serverContext = new DefaultServerRuntimeContext(serverEntity, relay);
        relay.setServerRuntimeContext(serverContext);
        TestSessionContext tsc = new TestSessionContext(serverContext, sessionStateHolder, relay);

        configureStorageProvider(tsc);
        configureServiceRegistry(tsc);
        return tsc;
    }

    protected void configureServiceRegistry(TestSessionContext tsc) {
        ServiceCollector serviceCollector = new ServiceCollector();
        ((DefaultServerRuntimeContext) tsc.getServerRuntimeContext())
                .registerServerRuntimeContextService(serviceCollector);
    }

    protected void configurePubsubModule(TestSessionContext tsc, PubSubServiceConfiguration serviceConfiguration) {
        ((DefaultServerRuntimeContext) tsc.getServerRuntimeContext())
                .addModule(new PublishSubscribeModule(serviceConfiguration));
    }

    protected void configureStorageProvider(TestSessionContext tsc) {
        OpenStorageProviderRegistry storageProviderRegistry = new OpenStorageProviderRegistry();
        storageProviderRegistry.add(new CollectionNodeInMemoryStorageProvider());
        storageProviderRegistry.add(new LeafNodeInMemoryStorageProvider());
        ((DefaultServerRuntimeContext) tsc.getServerRuntimeContext())
                .setStorageProviderRegistry(storageProviderRegistry);
    }

    private void setResourceConnected(String boundResourceId) {
        sessionContext.getServerRuntimeContext().getResourceRegistry().setResourceState(boundResourceId,
                ResourceState.CONNECTED);
    }

    /**
     * Override and provide the Handler to be tested. A new handler will be created
     * for each test.
     * 
     * @return the instantiated handler to be tested
     */
    protected abstract IQHandler getHandler();

    /**
     * Return the StanzaGenerator that build the "typical" request for the given
     * type of request.
     * 
     * This will be used to test whether the handler correctly accepts these
     * stanzas.
     * 
     * @return the default Stanza generator for the request type.
     */
    protected abstract AbstractStanzaGenerator getDefaultStanzaGenerator();

    protected Stanza sendStanza(Stanza toSend, boolean isOutboundStanza) {
        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker(new DefaultStanzaBroker(sessionContext.getStanzaRelay(), sessionContext));
        handler.execute(toSend, sessionContext.getServerRuntimeContext(), isOutboundStanza, sessionContext, null,
                stanzaBroker);
        return stanzaBroker.getUniqueStanzaWrittenToSession();
    }

    public void testSimpleStanza() {
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService, "id1", "sometestnode");

        assertTrue(handler.verify(stanza));
    }
}
