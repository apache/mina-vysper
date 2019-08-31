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

import java.net.InetSocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.vysper.mina.MinaBackedSessionContext;
import org.apache.vysper.mina.StanzaLoggingFilter;
import org.apache.vysper.mina.codec.XMPPProtocolCodecFactory;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerTimeoutException;
import org.apache.vysper.xmpp.modules.extension.xep0199_xmppping.XmppPingListener;
import org.apache.vysper.xmpp.modules.extension.xep0199_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DbResultHandler;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DbVerifyHandler;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DialbackIdGenerator;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.server.s2s.XmppEndpointResolver.ResolvedAddress;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link XMPPServerConnector}
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultXMPPServerConnector implements XmppPingListener, XMPPServerConnector {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultXMPPServerConnector.class);

    private final static List<StanzaHandler> S2S_HANDSHAKE_HANDLERS = Arrays.asList(new DbVerifyHandler(),
            new DbResultHandler(), new TlsProceedHandler(), new FeaturesHandler());

    private final ServerRuntimeContext serverRuntimeContext;

    private final StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory;

    private final StanzaProcessor stanzaProcessor;

    private MinaBackedSessionContext sessionContext;

    private final Entity remoteServer;

    private final SessionStateHolder sessionStateHolder = new SessionStateHolder();

    private IoConnector connector;

    private int connectTimeout = 30000;

    private int xmppHandshakeTimeout = 30000;

    private int pingPeriod = 30000;

    private int pingTimeout = 10000;

    private boolean closed = false;

    private SessionContext dialbackSessionContext;

    private SessionStateHolder dialbackSessionStateHolder;

    private Timer pingTimer;

    protected ServerConnectorIoHandler serverConnectorIoHandler;

    protected final CountDownLatch authenticatedLatch = new CountDownLatch(1);

    public DefaultXMPPServerConnector(Entity remoteServer, ServerRuntimeContext serverRuntimeContext,
            StanzaHandlerExecutorFactory stanzaHandlerExecutorFactory, StanzaProcessor stanzaProcessor,
            SessionContext dialbackSessionContext, SessionStateHolder dialbackSessionStateHolder) {
        this.serverRuntimeContext = serverRuntimeContext;
        this.stanzaProcessor = stanzaProcessor;
        this.stanzaHandlerExecutorFactory = stanzaHandlerExecutorFactory;
        this.remoteServer = remoteServer;
        this.dialbackSessionContext = dialbackSessionContext;
        this.dialbackSessionStateHolder = dialbackSessionStateHolder;
    }

    /**
     * Connect and authenticate the XMPP server connector
     */
    public synchronized void start() throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        LOG.info("Starting XMPP server connector to {}", remoteServer);

        boolean successfullyConnected = false;

        XmppEndpointResolver resolver = new XmppEndpointResolver();
        List<ResolvedAddress> addresses = resolver.resolveXmppServer(remoteServer.getDomain());

        Throwable lastException = null;

        if (!addresses.isEmpty()) {
            LOG.info("resolved {} address(es) for {}", addresses.size(), remoteServer);
            for (ResolvedAddress address : addresses) {
                final InetSocketAddress ipAddress = address.getAddress();
                LOG.info("Connecting to XMPP server {} at {}", remoteServer, ipAddress);

                connector = createConnector();
                ConnectFuture connectFuture = connector.connect(ipAddress);
                if (connectFuture.awaitUninterruptibly(connectTimeout) && connectFuture.isConnected()) {
                    // success on the TCP/IP level, now wait for the XMPP handshake
                    LOG.info("XMPP server {} connected at {}", remoteServer, ipAddress);
                    try {
                        if (authenticatedLatch.await(xmppHandshakeTimeout, TimeUnit.MILLISECONDS)) {
                            // success, break out of connect loop
                            successfullyConnected = true;
                            break;
                        } else {
                            // attempt next
                            LOG.warn("XMPP handshake with {} at {} timed out", remoteServer, ipAddress);
                        }
                    } catch (InterruptedException e) {
                        throw new RemoteServerTimeoutException("XMPPConnection to " + remoteServer + " was interrupted",
                                e);
                    }
                }

                lastException = connectFuture.getException();
                LOG.warn("Failed connecting to XMPP server " + remoteServer + " at " + ipAddress,
                        connectFuture.getException());
                disposeAndNullifyConnector();
            }
        } else {
            // should never happen
            throw new RemoteServerNotFoundException("DNS lookup of remote server failed");
        }

        if (!successfullyConnected) {
            String exceptionMsg = "Failed to connect to XMPP server at " + remoteServer;

            if (lastException instanceof UnresolvedAddressException) {
                throw new RemoteServerNotFoundException(exceptionMsg);
            } else {
                throw new RemoteServerTimeoutException(exceptionMsg);
            }

        }
    }

    private void disposeAndNullifyConnector() {
        IoConnector localConnector = connector;
        if (localConnector == null)
            return;
        localConnector.dispose();
        connector = null;
    }

    private NioSocketConnector createConnector() {
        NioSocketConnector connector = new NioSocketConnector();
        DefaultIoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
        filterChainBuilder.addLast("xmppCodec", new ProtocolCodecFilter(new XMPPProtocolCodecFactory()));
        filterChainBuilder.addLast("loggingFilter", new StanzaLoggingFilter());
        connector.setFilterChainBuilder(filterChainBuilder);
        serverConnectorIoHandler = new ServerConnectorIoHandler(remoteServer, this);
        connector.setHandler(serverConnectorIoHandler);
        return connector;
    }

    private void startPinging() {
        // are pings not already running and is the XMPP ping module active?
        if (pingTimer == null && serverRuntimeContext.getModule(XmppPingModule.class) != null) {
            pingTimer = new Timer("pingtimer", true);
            pingTimer.schedule(new PingTask(), pingPeriod, pingPeriod);
        }
    }

    private StanzaHandler lookupS2SHandler(Stanza stanza) {
        for (StanzaHandler handler : S2S_HANDSHAKE_HANDLERS) {
            if (handler.verify(stanza)) {
                return handler;
            }
        }
        return null;
    }

    public void handleReceivedStanza(Stanza stanza) {

        // check for basic stanza handlers
        StanzaHandler s2sHandler = lookupS2SHandler(stanza);

        if (s2sHandler != null) {
            try {
                stanzaHandlerExecutorFactory.build(s2sHandler).execute(stanza, serverRuntimeContext, false,
                        sessionContext, sessionStateHolder);
            } catch (ProtocolException e) {
                return;
            }

            if (sessionStateHolder.getState() == SessionState.AUTHENTICATED) {
                LOG.info("XMPP server connector to {} authenticated", remoteServer);
                authenticatedLatch.countDown();

                // connection established, start pinging
                startPinging();
            }
            // none of the handlers matched, stream start is handled separately
        } else if (stanza.getName().equals("stream")) {
            sessionContext.setSessionId(stanza.getAttributeValue("id"));
            sessionContext.setInitiatingEntity(remoteServer);

            String version = stanza.getAttributeValue("version");
            if (version == null) {
                // old protocol, assume dialback
                String dailbackId = new DialbackIdGenerator().generate(remoteServer,
                        serverRuntimeContext.getServerEntity(), sessionContext.getSessionId());

                Stanza dbResult = new StanzaBuilder("result", NamespaceURIs.JABBER_SERVER_DIALBACK, "db")
                        .addAttribute("from", serverRuntimeContext.getServerEntity().getDomain())
                        .addAttribute("to", remoteServer.getDomain()).addText(dailbackId).build();
                write(dbResult);
            }

            if (dialbackSessionContext != null) {
                // connector is being used for dialback verification, don't do further
                // authentication
                sessionContext.putAttribute("DIALBACK_SESSION_CONTEXT", dialbackSessionContext);
                sessionContext.putAttribute("DIALBACK_SESSION_STATE_HOLDER", dialbackSessionStateHolder);

                sessionContext.setInitiatingEntity(remoteServer);
                sessionStateHolder.setState(SessionState.AUTHENTICATED);
                authenticatedLatch.countDown();
            }
        } else {

            if (sessionStateHolder.getState() != SessionState.AUTHENTICATED) {
                LOG.warn("regular stanza sent before s2s session to {} was authenticated, closing", remoteServer);
                sessionContext.close();
                return;
            }
            // only deliver messages to directly server directly
            if (!serverRuntimeContext.getServerEntity().equals(stanza.getTo())) {
                LOG.info("not handling messages to clients here received from {} to {}", remoteServer, stanza.getTo());
                sessionContext.close();
                return;
            }

            stanzaProcessor.processStanza(serverRuntimeContext, sessionContext, stanza, sessionStateHolder);
        }
    }

    public void handleSessionSecured() {
        // connection secured, send stream opener
        sessionStateHolder.setState(SessionState.ENCRYPTED);

        LOG.info("XMPP server connector to {} secured using TLS", remoteServer);
        LOG.debug("XMPP server connector to {} restarting stream", remoteServer);

        sessionContext.setIsReopeningXMLStream();

        Stanza opener = new ServerResponses().getStreamOpenerForServerConnector(serverRuntimeContext.getServerEntity(),
                remoteServer, XMPPVersion.VERSION_1_0, sessionContext);

        sessionContext.write(opener);
    }

    public void handleSessionOpened(IoSession session) {
        LOG.info("XMPP server session opened to {}", remoteServer);
        sessionContext = new MinaBackedSessionContext(serverRuntimeContext, stanzaProcessor, sessionStateHolder,
                session);
        sessionStateHolder.setState(SessionState.INITIATED);
        Stanza opener = new ServerResponses().getStreamOpenerForServerConnector(serverRuntimeContext.getServerEntity(),
                remoteServer, XMPPVersion.VERSION_1_0, sessionContext);
        sessionContext.write(opener);
    }

    /**
     * {@inheritDoc}
     */
    public void write(Stanza stanza) {
        sessionContext.write(stanza);
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        LOG.info("XMPP server connector socket closed, closing connector (current status: {})",
                closed ? "closed" : "open");
        try {
            if (!closed) {
                LOG.info("XMPP server connector to {} is closing", remoteServer);
                if (pingTimer != null)
                    pingTimer.cancel();
                sessionContext.close();
                disposeAndNullifyConnector();
                LOG.info("XMPP server connector to {} is closed", remoteServer);
            }
        } finally {
            closed = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pong() {
        // do nothing, all happy
    }

    /**
     * {@inheritDoc}
     */
    public void timeout() {
        LOG.debug("XMPP server connector to {} timed out, closing", remoteServer);
        close();
    }

    /**
     * Is this XMPP server connector closed?
     * 
     * @return true if the connector is closed
     */
    public boolean isClosed() {
        return closed;
    }

    private class PingTask extends TimerTask {
        @Override
        public void run() {
            XmppPingModule pingModule = serverRuntimeContext.getModule(XmppPingModule.class);
            LOG.info("pinging federated XMPP server {}", remoteServer);
            pingModule.ping(DefaultXMPPServerConnector.this, serverRuntimeContext.getServerEntity(), remoteServer,
                    pingTimeout, DefaultXMPPServerConnector.this);
        }
    }
}
