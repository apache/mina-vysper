package org.apache.vysper.xmpp.server.s2s;

import java.io.IOException;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslEvent;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handler for server-to-server connections
 */
public class ServerConnectorIoHandler extends IoHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ServerConnectorIoHandler.class);

    protected final Entity remoteServer;

    protected final XMPPServerConnector serverConnector;

    ServerConnectorIoHandler(Entity remoteServer, XMPPServerConnector serverConnector) {
        this.remoteServer = remoteServer;
        this.serverConnector = serverConnector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            if (cause instanceof javax.net.ssl.SSLHandshakeException) {
                LOG.warn("failed to complete SSL handshake with server {}: {}", remoteServer, cause.getMessage());
            } else if (cause instanceof javax.net.ssl.SSLException) {
                LOG.warn("failure in SSL with server {}: {}", remoteServer, cause.getMessage());
            } else {
                LOG.info("I/O exception with server {}: {}", remoteServer, cause.getMessage());
            }
            serverConnector.close();
        } else {
            LOG.warn("Exception {} thrown by XMPP server connector to " + remoteServer
                    + ", probably a bug in Vysper: {}", cause.getClass().getName(), cause.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(IoSession session, Object message) {
        if (message == SslEvent.SECURED) {
            serverConnector.handleSessionSecured();
        } else if (message == SslEvent.UNSECURED) {
            // unsecured, closing
            serverConnector.close();
        } else if (message instanceof Stanza) {
            Stanza stanza = (Stanza) message;
            serverConnector.handleReceivedStanza(stanza);
        } else {
            throw new RuntimeException("Only handles SSL events and stanzas, got: " + message.getClass());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        // Socket was closed, make sure we close the connector
        serverConnector.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        serverConnector.handleSessionOpened(session);
    }
}
