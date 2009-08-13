package org.apache.vysper.spring;

import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.storage.StorageProviderRegistry;

import java.util.List;

/**
 */
public class SpringCompatibleDefaultServerRuntimeContext extends DefaultServerRuntimeContext {
    public SpringCompatibleDefaultServerRuntimeContext(Entity serverEntity, StanzaRelay stanzaRelay) {
        super(serverEntity, stanzaRelay);
    }

    public SpringCompatibleDefaultServerRuntimeContext(Entity serverEntity, StanzaRelay stanzaRelay, StorageProviderRegistry storageProviderRegistry) {
        super(serverEntity, stanzaRelay, storageProviderRegistry);
    }

    public SpringCompatibleDefaultServerRuntimeContext(Entity serverEntity, StanzaRelay stanzaRelay, ServerFeatures serverFeatures, List<NamespaceHandlerDictionary> dictionaries, ResourceRegistry resourceRegistry) {
        super(serverEntity, stanzaRelay, serverFeatures, dictionaries, resourceRegistry);
    }

    public void setModules(List<Module> modules) {
        super.addModules(modules);
    }
}
