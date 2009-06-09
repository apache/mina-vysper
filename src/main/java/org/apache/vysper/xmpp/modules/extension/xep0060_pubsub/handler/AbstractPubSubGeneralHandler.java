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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanza;

/**
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public abstract class AbstractPubSubGeneralHandler extends
		AbstractPublishSubscribeIQHandler {

	/**
	 * @param root
	 */
	public AbstractPubSubGeneralHandler(CollectionNode root) {
		super(root);
	}

	@Override
	protected String getNamespace() {
		return NamespaceURIs.XEP0060_PUBSUB;
	}

	/**
	 * Extracts the node name from a given IQ stanza. The node attribute
	 * takes precedence over the JID resource. The standard requires only
	 * one of these addressing methods.
	 * 
	 * @param stanza the received IQStanza
	 * @return the node
	 */
	protected Entity extractNodeJID(IQStanza stanza) {
		String node = stanza.getFirstInnerElement().getAttributeValue("node");
		if(node == null) {
			return stanza.getTo();
		} else {
			Entity to = stanza.getTo();
			return new EntityImpl(to.getNode(), to.getDomain(), node);
		}
	}
	
}
