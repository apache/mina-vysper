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

import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubCreateNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubPublishHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubSubscribeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubUnsubscribeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubOwnerConfigureNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubOwnerDeleteNodeHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
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
		
        PubSubPersistenceManager persistenceManager = (PubSubPersistenceManager) serverRuntimeContext.getStorageProvider(PubSubPersistenceManager.class);
        if (persistenceManager == null) {
            logger.error("No persistency manager found");
            // TODO throw some exception - without PM we can't do anything useful
        } else {
        	root.setPersistenceManager(persistenceManager);
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
    
	public List<InfoElement> getServerInfosFor(InfoRequest request)
			throws ServiceDiscoveryRequestException {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("pubsub", "service"));
        infoElements.add(new Feature(NamespaceURIs.XEP0060_PUBSUB));
        return infoElements;
	}
	
    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        ArrayList<StanzaHandler> pubsubHandlers = new ArrayList<StanzaHandler>();
        pubsubHandlers.add(new PubSubSubscribeHandler(root));
        pubsubHandlers.add(new PubSubUnsubscribeHandler(root));
        pubsubHandlers.add(new PubSubPublishHandler(root));
        pubsubHandlers.add(new PubSubCreateNodeHandler(root));
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB, pubsubHandlers));
        
        ArrayList<StanzaHandler> pubsubOwnerHandlers = new ArrayList<StanzaHandler>();
        pubsubOwnerHandlers.add(new PubSubOwnerConfigureNodeHandler(root));
        pubsubOwnerHandlers.add(new PubSubOwnerDeleteNodeHandler(root));
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB_OWNER, pubsubOwnerHandlers));
    }

}
