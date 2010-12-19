package org.apache.vysper.xmpp.server.s2s;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

public class XMPPServerConnectorRegistry {

    private ServerRuntimeContext serverRuntimeContext;
    private Map<Entity, XMPPServerConnector> connectors = new ConcurrentHashMap<Entity, XMPPServerConnector>();
    
    public synchronized XMPPServerConnector getConnector(Entity server) {
        XMPPServerConnector connector = connectors.get(server);

        // TODO handle closed connectors
        if(connector == null) {
            connector = new XMPPServerConnector(server, serverRuntimeContext);
            connector.start();
            connectors.put(server, connector);
        }
        
        return connector;
    }
}
