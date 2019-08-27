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
import org.apache.vysper.xmpp.protocol.StanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link XMPPServerConnectorRegistry}
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultXMPPServerConnectorRegistry implements XMPPServerConnectorRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultXMPPServerConnectorRegistry.class);

    private final ServerRuntimeContext serverRuntimeContext;

    private final Map<Entity, XMPPServerConnector> connectors = new ConcurrentHashMap<>();

    private final StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory;

    private final StanzaProcessor stanzaProcessor;

    public DefaultXMPPServerConnectorRegistry(ServerRuntimeContext serverRuntimeContext,
            StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory,
            StanzaProcessor stanzaProcessor) {
        this.serverRuntimeContext = serverRuntimeContext;
        this.stanzaHandlerExecutorFactory = stanzaHandlerExecutorFactory;
        this.stanzaProcessor = stanzaProcessor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry#getConnector(
     * org.apache.vysper.xmpp.addressing.Entity)
     */
    @SpecCompliant(spec = "draft-ietf-xmpp-3920bis-22", section = "10.4", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
    public synchronized XMPPServerConnector connect(Entity server)
            throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        XMPPServerConnector connector = connectors.get(server);

        if (connector != null && connector.isClosed()) {
            connectors.remove(server);
            connector = null;
        }

        if (connector == null) {
            LOG.debug("starting s2s connector to " + server);
            connector = createConnector(server, serverRuntimeContext, null, null);
            // TODO start async, because we are in a synchronized method
            connector.start(); // this can take some time to complete
            LOG.debug("started s2s connector to " + server);

            connectors.put(server, connector);
            LOG.debug("s2s connector registry size is now " + connectors.size());
        }

        return connector;
    }

    @SpecCompliant(spec = "draft-ietf-xmpp-3920bis-22", section = "10.4", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
    public synchronized XMPPServerConnector connectForDialback(Entity server, SessionContext orginalSessionContext,
            SessionStateHolder originalSessionStateHolder)
            throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        LOG.debug("starting s2s connector for dialback to " + server);
        XMPPServerConnector connector = createConnector(server, serverRuntimeContext, orginalSessionContext,
                originalSessionStateHolder);
        connector.start();
        LOG.debug("started s2s connector for dialback to " + server);
        return connector;
    }

    protected XMPPServerConnector createConnector(Entity otherServer, ServerRuntimeContext serverRuntimeContext,
            SessionContext dialbackSessionContext, SessionStateHolder dialbackSessionStateHolder) {
        return new DefaultXMPPServerConnector(otherServer, serverRuntimeContext, stanzaHandlerExecutorFactory,
                stanzaProcessor, dialbackSessionContext, dialbackSessionStateHolder);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.vysper.xmpp.server.s2s.XMPPServerConnectorRegistry#close()
     */
    public synchronized void close() {
        LOG.debug("closing now all {} s2s connectors", connectors.size());
        for (XMPPServerConnector connector : connectors.values()) {
            try {
                connector.close();
            } catch (Throwable e) {
                e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

}
