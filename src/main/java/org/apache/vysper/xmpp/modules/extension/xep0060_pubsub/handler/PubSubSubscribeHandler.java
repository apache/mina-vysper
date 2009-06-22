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
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;


/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class PubSubSubscribeHandler extends AbstractPubSubGeneralHandler {

	/**
	 * @param root
	 */
	public PubSubSubscribeHandler(CollectionNode root) {
		super(root);
	}

	@Override
	protected String getWorkerElement() {
		return "subscribe";
	}

	@Override
	protected Stanza handleSet(IQStanza stanza,
			ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext) {
		Entity sender = stanza.getFrom();
		Entity receiver = stanza.getTo();
		Entity subJID = null;

		String iqStanzaID = stanza.getAttributeValue("id");
		
		StanzaBuilder sb = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.RESULT, iqStanzaID);
		sb.startInnerElement("pubsub", NamespaceURIs.XEP0060_PUBSUB);
		
		XMLElement sub = stanza.getFirstInnerElement().getFirstInnerElement(); // pubsub/subscribe
		String strSubJID = sub.getAttributeValue("jid"); // MUST
		
		try {
			subJID = EntityImpl.parse(strSubJID);
		} catch (EntityFormatException e) {
			return errorStanzaGenerator.generateJIDMalformedErrorStanza(sender, receiver, iqStanzaID);
		}
		
		if(!sender.getBareJID().equals(subJID.getBareJID())) {
			// error condition 1 (6.1.3)
			return errorStanzaGenerator.generateJIDDontMatchErrorStanza(sender, receiver, iqStanzaID);
		}
		
		Entity nodeJID = extractNodeJID(stanza);
		LeafNode node = root.find(nodeJID);
		
		if(node == null) {
			// no such node (error condition 11 (6.1.3))
			return errorStanzaGenerator.generateNoNodeErrorStanza(sender, receiver, iqStanzaID);
		}
		
		String id = idGenerator.create();
		node.subscribe(id, subJID);
		
		buildSuccessStanza(sb, nodeJID, strSubJID, id);
		
		sb.endInnerElement(); // pubsub
		return new IQStanza(sb.getFinalStanza());
	}

	
	private void buildSuccessStanza(StanzaBuilder sb, Entity node, String jid, String subid) {
		sb.startInnerElement("subscription");
		sb.addAttribute("node", node.getResource());
		sb.addAttribute("jid", jid);
		sb.addAttribute("subid", subid);
		sb.addAttribute("subscription", "subscribed");
		sb.endInnerElement();
	}
}
