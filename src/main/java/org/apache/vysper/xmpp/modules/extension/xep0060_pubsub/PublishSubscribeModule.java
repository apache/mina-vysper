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

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubCreateNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubPublishHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubSubscribeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubUnsubscribeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubOwnerConfigureNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubOwnerDeleteNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the XEP0060 module.
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public class PublishSubscribeModule extends DefaultDiscoAwareModule implements ServerInfoRequestListener {

    CollectionNode root = null;
    final Logger logger = LoggerFactory.getLogger(PublishSubscribeModule.class);

    /**
     * Default constructor takes care of the root-CollectionNode
     */
    public PublishSubscribeModule() {
        this.root = new CollectionNode();
    }

    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);

        CollectionNodeStorageProvider collectionNodeStorageProvider = (CollectionNodeStorageProvider) serverRuntimeContext.getStorageProvider(CollectionNodeStorageProvider.class);
        LeafNodeStorageProvider leafNodeStorageProvider = (LeafNodeStorageProvider) serverRuntimeContext.getStorageProvider(LeafNodeStorageProvider.class);
        
        if (collectionNodeStorageProvider == null) {
            logger.error("No collection node storage provider found, using the default (in memory)");
        } else {
            root.setCollectionNodeStorageProvider(collectionNodeStorageProvider);
        }

        if (leafNodeStorageProvider == null) {
            logger.error("No leaf node storage provider found, using the default (in memory)");
        } else {
            root.setLeafNodeStorageProvider(leafNodeStorageProvider);
        }
    }

    @Override
    public String getName() {
        return "XEP-0060 Publish-Subscribe";
    }

    @Override
    public String getVersion() {
        return "1.13rc3";
    }

    @Override
    protected void addServerInfoRequestListeners(List<ServerInfoRequestListener> serverInfoRequestListeners) {
        serverInfoRequestListeners.add(this);
    }

    /**
     * Implements the getServerInfosFor method from the {@link ServerInfoRequestListener} interface.
     * Makes this modules available via disco as "pubsub service" in the pubsub namespace.
     * 
     * @see ServerInfoRequestListener#getServerInfosFor(InfoRequest)
     */
    public List<InfoElement> getServerInfosFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("pubsub", "service"));
        infoElements.add(new Feature(NamespaceURIs.XEP0060_PUBSUB));
        return infoElements;
    }

    /**
     * Registers the handlers for the various stanza types known to this pubsub implementation.
     * 
     * @see DefaultModule#addHandlerDictionaries(List<HandlerDictionary> dictionary)
     */
    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        addPubsubHandlers(dictionary);
        addPubsubOwnerHandlers(dictionary);
    }

    /**
     * Inserts the handlers for the pubsub#owner namespace into the HandlerDictionary.
     * @param dictionary the list to which the handlers should be appended.
     */
    private void addPubsubOwnerHandlers(List<HandlerDictionary> dictionary) {
        ArrayList<StanzaHandler> pubsubOwnerHandlers = new ArrayList<StanzaHandler>();
        pubsubOwnerHandlers.add(new PubSubOwnerConfigureNodeHandler(root));
        pubsubOwnerHandlers.add(new PubSubOwnerDeleteNodeHandler(root));
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB_OWNER, pubsubOwnerHandlers));
    }

    /**
     * Inserts the handlers for the pubsub namespace into the HandlerDictionary.
     * @param dictionary the list to which the handlers should be appended.
     */
    private void addPubsubHandlers(List<HandlerDictionary> dictionary) {
        ArrayList<StanzaHandler> pubsubHandlers = new ArrayList<StanzaHandler>();
        pubsubHandlers.add(new PubSubSubscribeHandler(root));
        pubsubHandlers.add(new PubSubUnsubscribeHandler(root));
        pubsubHandlers.add(new PubSubPublishHandler(root));
        pubsubHandlers.add(new PubSubCreateNodeHandler(root));
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB, pubsubHandlers));
    }

}
