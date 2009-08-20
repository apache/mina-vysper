package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoInfoIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaHandler;

/**
 */
public class MUCOccupantInfoDiscoTestCase extends AbstractMUCOccupantDiscoTestCase {

    protected String getNamespace() {
        return NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO;
    }
    
    @Override
    protected StanzaHandler createHandler() {
        return new DiscoInfoIQHandler();
    }
}
