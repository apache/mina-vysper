package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoItemIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaHandler;

/**
 */
public class MUCOccupantDiscoItemsTestCase extends AbstractMUCOccupantDiscoTestCase {

    protected String getNamespace() {
        return NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS;
    }
    
    @Override
    protected StanzaHandler createHandler() {
        return new DiscoItemIQHandler();
    }
}
