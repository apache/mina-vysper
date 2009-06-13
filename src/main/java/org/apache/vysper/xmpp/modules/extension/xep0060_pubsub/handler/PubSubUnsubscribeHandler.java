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
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.MultipleSubscriptionException;
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
public class PubSubUnsubscribeHandler extends AbstractPubSubGeneralHandler {

	/**
	 * @param root
	 */
	public PubSubUnsubscribeHandler(CollectionNode root) {
		super(root);
	}

	@Override
	protected String getWorkerElement() {
		return "unsubscribe";
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
		
		XMLElement unsub = stanza.getFirstInnerElement().getFirstInnerElement(); // pubsub/unsubscribe
		String strSubJID = unsub.getAttributeValue("jid"); // MUST
		String strSubID = unsub.getAttributeValue("subid"); // SHOULD (req. for more than one subscription)
		
		try {
			subJID = EntityImpl.parse(strSubJID);
		} catch (EntityFormatException e) {
			// TODO return error stanza... (general error)
			return null;
		}
		
		if(!sender.getBareJID().equals(subJID.getBareJID())) {
			// insufficient privileges (error condition 3 (6.2.3))
			return generateInsufficientPrivilegesErrorStanza(sender, receiver, iqStanzaID);
		}
				
		Entity nodeJID = extractNodeJID(stanza);
		LeafNode node = root.find(nodeJID);
		
		if(node == null) {
			// no such node (error condition 4 (6.2.3))
			return generateNoNodeErrorStanza(sender, receiver, iqStanzaID);
		}
		
		if(strSubID == null) {
			try {
				if(node.unsubscribe(subJID) == false) {
					// has no subscription (6.2.3.2)
					return generateNoSuchSubscriberErrorStanza(sender, receiver, iqStanzaID);
				}
			} catch(MultipleSubscriptionException e) {
				// error case 6.2.3.1
				return generateSubIDRequiredErrorStanza(sender, receiver, iqStanzaID);
			}
		} else {
			if(node.unsubscribe(strSubID, subJID) == false) {
				// subID not valid (6.2.3.5)
				return generateSubIDNotValidErrorStanza(sender, receiver, iqStanzaID);
			}
		}
		
		sb.endInnerElement(); // pubsub
		return new IQStanza(sb.getFinalStanza());
	}

	private Stanza generateInsufficientPrivilegesErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
		StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
		error.startInnerElement("error");
		error.addAttribute("type", "auth");
		error.startInnerElement("forbidden", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
		error.endInnerElement(); // forbidden
		error.endInnerElement(); // error
		return error.getFinalStanza();
	}

	private Stanza generateNoSuchSubscriberErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
		StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
		error.startInnerElement("error");
		error.addAttribute("type", "cancel");
		error.startInnerElement("unexpected-request", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
		error.endInnerElement(); // unexpected-request
		error.startInnerElement("not-subscribed", NamespaceURIs.XEP0060_PUBSUB_ERRORS);
		error.endInnerElement(); // not-subscribed
		error.endInnerElement(); // error
		return error.getFinalStanza();
	}

	private Stanza generateSubIDRequiredErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
		StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
		error.startInnerElement("error");
		error.addAttribute("type", "modify");
		error.startInnerElement("bad-request", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
		error.endInnerElement(); // bad-request
		error.startInnerElement("subid-required", NamespaceURIs.XEP0060_PUBSUB_ERRORS);
		error.endInnerElement(); // subid-required
		error.endInnerElement(); // error
		return error.getFinalStanza();
	}
	
	private Stanza generateNoNodeErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
		StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
		error.startInnerElement("error");
		error.addAttribute("type", "cancel");
		error.startInnerElement("item-does-not-exist", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
		error.endInnerElement(); // item-does-not-exist
		error.endInnerElement(); // error
		return error.getFinalStanza();
	}

	private Stanza generateSubIDNotValidErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
		StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
		error.startInnerElement("error");
		error.addAttribute("type", "modify");
		error.startInnerElement("not-acceptable", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
		error.endInnerElement(); // not-acceptable
		error.startInnerElement("invalid-subid", NamespaceURIs.XEP0060_PUBSUB_ERRORS);
		error.endInnerElement(); // invlaid-subid
		error.endInnerElement(); // error
		return error.getFinalStanza();
	}
}
