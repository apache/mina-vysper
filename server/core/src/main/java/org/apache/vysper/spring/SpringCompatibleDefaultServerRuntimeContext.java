package org.apache.vysper.spring;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

import java.util.List;

/**
 */
public class SpringCompatibleDefaultServerRuntimeContext extends DefaultServerRuntimeContext {
    
    private static class StanzaRelayHull implements StanzaRelay {
        
        protected StanzaRelay stanzaRelay;

        public void setStanzaRelay(StanzaRelay stanzaRelay) {
            this.stanzaRelay = stanzaRelay;
        }

        public void relay(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy) throws DeliveryException {
            stanzaRelay.relay(receiver, stanza, deliveryFailureStrategy);
        }
    }
    
    public SpringCompatibleDefaultServerRuntimeContext(Entity serverEntity, ServerFeatures serverFeatures, List<NamespaceHandlerDictionary> dictionaries, ResourceRegistry resourceRegistry) {
        super(serverEntity, new StanzaRelayHull(), serverFeatures, dictionaries, resourceRegistry);
    }
    
    public void setStanzaRelay(StanzaRelay stanzaRelay) {
        ((StanzaRelayHull)getStanzaRelay()).setStanzaRelay(stanzaRelay);
    }

    public void setModules(List<Module> modules) {
        super.addModules(modules);
    }
}
