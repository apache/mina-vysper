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
package org.apache.vysper.xmpp.server.s2s;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerTimeoutException;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;

/**
 * Default implementation of {@link XMPPServerConnectorRegistry} 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultXMPPServerConnectorRegistry implements XMPPServerConnectorRegistry {

    private ServerRuntimeContext serverRuntimeContext;
    private Map<Entity, DefaultXMPPServerConnector> connectors = new ConcurrentHashMap<Entity, DefaultXMPPServerConnector>();
    
    public DefaultXMPPServerConnectorRegistry(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /* (non-Javadoc)
     * @see org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry#getConnector(org.apache.vysper.xmpp.addressing.Entity)
     */
    @SpecCompliant(spec = "draft-ietf-xmpp-3920bis-22", section = "10.4", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
    public synchronized XMPPServerConnector connect(Entity server) throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        DefaultXMPPServerConnector connector = connectors.get(server);

        if(connector != null && connector.isClosed()) {
            connectors.remove(server);
            connector = null;
        } 
        
        if(connector == null) {
            connector = new DefaultXMPPServerConnector(server, serverRuntimeContext, null, null);
            connector.start();

            connectors.put(server, connector);
        }
        
        return connector;        
    }
    
    @SpecCompliant(spec = "draft-ietf-xmpp-3920bis-22", section = "10.4", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
    public synchronized XMPPServerConnector connectForDialback(Entity server, SessionContext orginalSessionContext, SessionStateHolder originalSessionStateHolder) throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        DefaultXMPPServerConnector connector = new DefaultXMPPServerConnector(server, serverRuntimeContext, orginalSessionContext, originalSessionStateHolder);
        connector.start();
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
