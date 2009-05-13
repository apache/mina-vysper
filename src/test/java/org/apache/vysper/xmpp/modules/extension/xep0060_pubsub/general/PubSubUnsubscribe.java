package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.general;

import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeTestCase;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PubSubUnsubscribe extends AbstractPublishSubscribeTestCase {

	@Override
	protected StanzaBuilder buildInnerElement(StanzaBuilder sb) {
		sb.startInnerElement("unsubscribe");
		sb.addAttribute("node", pubsub.getResource());
		sb.addAttribute("jid", client.getFullQualifiedName());
		sb.endInnerElement();
		return sb;
	}

	@Override
	protected IQHandler getHandler() {
		return new PubSubUnsubscribeHandler();
	}

	@Override
	protected String getNamespace() {
		return NamespaceURIs.XEP0060_PUBSUB;
	}

	@Override
	protected IQStanzaType getStanzaType() {
		return IQStanzaType.SET;
	}

}
