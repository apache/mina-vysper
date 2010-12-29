package org.apache.vysper.xmpp.server.s2s;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

public class XMPPServerConnectorRegistry {

    private ServerRuntimeContext serverRuntimeContext;
    private Map<Entity, XMPPServerConnector> connectors = new ConcurrentHashMap<Entity, XMPPServerConnector>();
    
    public XMPPServerConnectorRegistry(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public synchronized XMPPServerConnector getConnector(Entity server) {
        XMPPServerConnector connector = connectors.get(server);

        if(connector != null && connector.isClosed()) {
            connectors.remove(server);
            connector = null;
        } 
        
        if(connector == null) {
            connector = new XMPPServerConnector(server, serverRuntimeContext);
            connector.start();
            connectors.put(server, connector);
        }
        
        return connector;
    }

    public void close() {
        for(XMPPServerConnector connector : connectors.values()) {
            connector.close();
        }
    }
}
