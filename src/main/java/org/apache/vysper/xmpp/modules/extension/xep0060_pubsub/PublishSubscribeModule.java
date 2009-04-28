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

import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes the XEP0060 module.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class PublishSubscribeModule extends DefaultDiscoAwareModule implements
		ServerInfoRequestListener {

	@Override
	public void initialize(ServerRuntimeContext serverRuntimeContext) {
		super.initialize(serverRuntimeContext);
	}
	
	@Override
	public String getName() {
		return "XEP-0060 Publish-Subscribe";
	}

	@Override
	public String getVersion() {
		return "1.13rc3";
	}

	public List<InfoElement> getServerInfosFor(InfoRequest request)
			throws ServiceDiscoveryRequestException {
        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Feature(NamespaceURIs.XEP0060_PUBSUB));
        infoElements.add(new Feature(NamespaceURIs.XEP0060_PUBSUB_OWNER));
        return infoElements;
	}
	
    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB));
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0060_PUBSUB_OWNER));
    }

}
