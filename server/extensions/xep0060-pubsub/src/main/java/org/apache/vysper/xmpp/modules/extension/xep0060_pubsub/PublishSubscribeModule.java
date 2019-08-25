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
import java.util.Collections;
import java.util.List;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.core.base.handler.MessageHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubCreateNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubPublishHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubRetrieveAffiliationsHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubRetrieveSubscriptionsHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubSubscribeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubUnsubscribeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubOwnerConfigureNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubOwnerDeleteNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeStorageProvider;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ComponentInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.components.ComponentStanzaProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the XEP0060 module. This class is also responsible for disco requests at the service level.
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", comment = "spec. version: 1.13rc", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class PublishSubscribeModule extends DefaultDiscoAwareModule implements Component, ComponentInfoRequestListener,
        ItemRequestListener {

    // The configuration of the service
    private PubSubServiceConfiguration serviceConfiguration = null;

    // for debugging
    private final Logger logger = LoggerFactory.getLogger(PublishSubscribeModule.class);

    private ServerRuntimeContext serverRuntimeContext;

    /**
     * the subdomain this module becomes know under.
     */
    protected String subdomain = "pubsub";

    /**
     * the domain derived from the subdomain and the server domain
     */
    protected Entity fullDomain;

    /**
     * Create a new PublishSubscribeModule together with a new root-collection node.
     */
    public PublishSubscribeModule(String subdomain) {
        this();
        this.subdomain = subdomain; 
    }

    /**
     * Create a new PublishSubscribeModule together with a new root-collection node.
     */
    public PublishSubscribeModule() {
        this(new PubSubServiceConfiguration(new CollectionNode()));
    }

    /**
     * Create a new PublishSubscribeModule together with a supplied root-collection node.
     */
    public PublishSubscribeModule(PubSubServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    /**
     * Initializes the pubsub module, configuring the storage providers.
     */
    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);

        this.serverRuntimeContext = serverRuntimeContext;

        fullDomain = EntityUtils.createComponentDomain(subdomain, serverRuntimeContext);

        CollectionNodeStorageProvider collectionNodeStorageProvider = serverRuntimeContext
                .getStorageProvider(CollectionNodeStorageProvider.class);
        LeafNodeStorageProvider leafNodeStorageProvider = serverRuntimeContext
                .getStorageProvider(LeafNodeStorageProvider.class);

        if (collectionNodeStorageProvider == null) {
            logger.warn("No collection node storage provider found, using the default (in memory)");
        } else {
            serviceConfiguration.setCollectionNodeStorageProvider(collectionNodeStorageProvider);
        }

        if (leafNodeStorageProvider == null) {
            logger.warn("No leaf node storage provider found, using the default (in memory)");
        } else {
            serviceConfiguration.setLeafNodeStorageProvider(leafNodeStorageProvider);
        }

        this.serviceConfiguration.setDomainJID(fullDomain);
        this.serviceConfiguration.initialize();
    }

    /**
     * Returns the service name
     */
    @Override
    public String getName() {
        return "XEP-0060 Publish-Subscribe";
    }

    /**
     * Returns the implemented spec. version.
     */
    @Override
    public String getVersion() {
        return "1.13rc3";
    }

    /**
     * Implements the getServerInfosFor method from the {@link ServerInfoRequestListener} interface.
     * Makes this modules available via disco#info as "pubsub service" in the pubsub namespace.
     * 
     * @see ComponentInfoRequestListener#getComponentInfosFor(InfoRequest, StanzaBroker) 
     */
    public List<InfoElement> getComponentInfosFor(InfoRequest request, StanzaBroker stanzaBroker) throws ServiceDiscoveryRequestException {
        if (!EntityUtils.isAddressingServer(fullDomain, request.getTo()))
            return null;

        CollectionNode root = serviceConfiguration.getRootNode();
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        if (request.getNode() == null || request.getNode().length() == 0) {
            infoElements.add(new Identity("pubsub", "service", "Publish-Subscribe"));
            infoElements.add(new Feature(NamespaceURIs.XEP0060_PUBSUB));
        } else {
            LeafNode node = root.find(request.getNode());
            infoElements.addAll(node.getNodeInfosFor(request));
        }
        return infoElements;
    }

    @Override
    protected void addComponentInfoRequestListeners(List<ComponentInfoRequestListener> componentInfoRequestListeners) {
        componentInfoRequestListeners.add(this);
    }

    /**
     * Make this object available for disco#items requests.
     */
    @Override
    protected void addItemRequestListeners(List<ItemRequestListener> itemRequestListeners) {
        itemRequestListeners.add(this);
    }

    /**
     * Implements the getItemsFor method from the {@link ItemRequestListener} interface.
     * Makes this modules available via disco#items and returns the associated nodes.
     * 
     * @see ItemRequestListener#getItemsFor(InfoRequest, StanzaBroker)
     */
    public List<Item> getItemsFor(InfoRequest request, StanzaBroker stanzaBroker) throws ServiceDiscoveryRequestException {
        CollectionNode root = serviceConfiguration.getRootNode();
        List<Item> items = null;

        if (request.getNode() == null || request.getNode().length() == 0) {
            if (serverRuntimeContext.getServerEntity().equals(request.getTo())) {
                // top level item request. for example return entry for "pubsub.vysper.org" on request for "vysper.org"
                List<Item> componentItem = new ArrayList<Item>();
                componentItem.add(new Item(fullDomain));
                return componentItem;
            } else if (!fullDomain.equals(request.getTo())) {
                return null; // not in component's domain
            }
            ServiceDiscoItemsVisitor nv = new ServiceDiscoItemsVisitor(serviceConfiguration);
            root.acceptNodes(nv);
            items = nv.getNodeItemList();
        } else {
            LeafNode node = root.find(request.getNode());
            NodeDiscoItemsVisitor iv = new NodeDiscoItemsVisitor(request.getTo());
            node.acceptItems(iv);
            items = iv.getItemList();
        }

        return items;
    }

    public String getSubdomain() {
        return subdomain;
    }

    @Override
    public List<StanzaHandler> getComponentHandlers(Entity fullDomain) {
        return Collections.emptyList();
    }

    @Override
    public List<NamespaceHandlerDictionary> getComponentHandlerDictionnaries(Entity fullDomain) {
        List<NamespaceHandlerDictionary> dictionaries = new ArrayList<>();
        addPubsubHandlers(dictionaries);
        addPubsubOwnerHandlers(dictionaries);
        dictionaries.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB_EVENT, new MessageHandler()));
        return dictionaries;
    }

    /**
     * Inserts the handlers for the pubsub#owner namespace into the HandlerDictionary.
     * @param dictionaries the list to which the handlers should be appended.
     */
    private void addPubsubOwnerHandlers(List<NamespaceHandlerDictionary> dictionaries) {
        ArrayList<StanzaHandler> pubsubOwnerHandlers = new ArrayList<>();
        pubsubOwnerHandlers.add(new PubSubOwnerConfigureNodeHandler(serviceConfiguration));
        pubsubOwnerHandlers.add(new PubSubOwnerDeleteNodeHandler(serviceConfiguration));
        dictionaries
                .add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB_OWNER, pubsubOwnerHandlers));
    }

    /**
     * Inserts the handlers for the pubsub namespace into the HandlerDictionary.
     * @param dictionaries the list to which the handlers should be appended.
     */
    private void addPubsubHandlers(List<NamespaceHandlerDictionary> dictionaries) {
        ArrayList<StanzaHandler> pubsubHandlers = new ArrayList<>();
        pubsubHandlers.add(new PubSubSubscribeHandler(serviceConfiguration));
        pubsubHandlers.add(new PubSubUnsubscribeHandler(serviceConfiguration));
        pubsubHandlers.add(new PubSubPublishHandler(serviceConfiguration));
        pubsubHandlers.add(new PubSubCreateNodeHandler(serviceConfiguration));
        pubsubHandlers.add(new PubSubRetrieveSubscriptionsHandler(serviceConfiguration));
        pubsubHandlers.add(new PubSubRetrieveAffiliationsHandler(serviceConfiguration));
        dictionaries.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB, pubsubHandlers));
    }

}
