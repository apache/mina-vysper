package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.owner;

import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeTestCase;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PubSubConfigureNode extends AbstractPublishSubscribeTestCase {

	@Override
	protected StanzaBuilder buildInnerElement(StanzaBuilder sb) {
		sb.startInnerElement("configure");
		sb.addAttribute("node", pubsub.getResource());
		sb.endInnerElement();
		return sb;
	}

	@Override
	protected IQHandler getHandler() {
		return new PubSubOwnerConfigureNodeHandler();
	}

	@Override
	protected String getNamespace() {
		return NamespaceURIs.XEP0060_PUBSUB_OWNER;
	}

	@Override
	protected IQStanzaType getStanzaType() {
		return IQStanzaType.GET;
	}

}
