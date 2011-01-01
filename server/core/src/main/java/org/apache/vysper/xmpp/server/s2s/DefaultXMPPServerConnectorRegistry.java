package org.apache.vysper.xmpp.server.s2s;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerTimeoutException;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

public class DefaultXMPPServerConnectorRegistry implements XMPPServerConnectorRegistry {

    private ServerRuntimeContext serverRuntimeContext;
    private Map<Entity, DefaultXMPPServerConnector> connectors = new ConcurrentHashMap<Entity, DefaultXMPPServerConnector>();
    
    public DefaultXMPPServerConnectorRegistry(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /* (non-Javadoc)
     * @see org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry#getConnector(org.apache.vysper.xmpp.addressing.Entity)
     */
    @SpecCompliant(spec = "draft-ietf-xmpp-3920bis-22", section = "10.4", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
    public synchronized XMPPServerConnector connect(Entity server) throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        DefaultXMPPServerConnector connector = connectors.get(server);

        if(connector != null && connector.isClosed()) {
            connectors.remove(server);
            connector = null;
        } 
        
        if(connector == null) {
            connector = new DefaultXMPPServerConnector(server, serverRuntimeContext);
            connector.start();
            connectors.put(server, connector);
        }
        
        return connector;
    }

    /* (non-Javadoc)
     * @see org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry#close()
     */
    public void close() {
        for(DefaultXMPPServerConnector connector : connectors.values()) {
            connector.close();
        }
    }
}
