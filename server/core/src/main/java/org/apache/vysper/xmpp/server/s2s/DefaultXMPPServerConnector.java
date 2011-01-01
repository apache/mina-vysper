package org.apache.vysper.xmpp.server.s2s;
import java.nio.channels.UnresolvedAddressException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.vysper.mina.MinaBackedSessionContext;
import org.apache.vysper.mina.StanzaLoggingFilter;
import org.apache.vysper.mina.codec.XMPPProtocolCodecFactory;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerTimeoutException;
import org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingListener;
import org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DailbackIdGenerator;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.server.s2s.XmppEndpointResolver.ResolvedAddress;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultXMPPServerConnector implements XmppPingListener, XMPPServerConnector {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultXMPPServerConnector.class);
    
    private ServerRuntimeContext serverRuntimeContext;
    private MinaBackedSessionContext sessionContext;
    private Entity otherServer;
    private SessionStateHolder sessionStateHolder = new SessionStateHolder();
    private IoConnector connector;
    
    private int connectTimeout = 30000;
    private int xmppHandshakeTimeout = 30000;

    private int pingPeriod = 30000;
    private int pingTimeout = 10000;
    
    private boolean closed = false;
    
    private Timer pingTimer = new Timer("pingtimer", true);
    
    public DefaultXMPPServerConnector(Entity otherServer, ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
        this.otherServer = otherServer;
    }

    public synchronized void start() throws RemoteServerNotFoundException, RemoteServerTimeoutException {
        LOG.info("Starting XMPP server connector to {}", otherServer);

        // make this method synchronous
        final CountDownLatch authenticatedLatch = new CountDownLatch(1);
        
        boolean successfullyConnected = false;
        
        XmppEndpointResolver resolver = new XmppEndpointResolver();
        List<ResolvedAddress> addresses = resolver.resolveXmppServer(otherServer.getDomain());
        
        Throwable lastException = null;
        
        if(!addresses.isEmpty()) {
            for(ResolvedAddress address : addresses) {
                LOG.info("Connecting to XMPP server {} at {}", otherServer, address.getAddress());
                
                connector = createConnector(authenticatedLatch);
                
                ConnectFuture connectFuture = connector.connect(address.getAddress());
                if(connectFuture.awaitUninterruptibly(connectTimeout) && connectFuture.isConnected()) {
                    // success on the TCP/IP lever, now wait for the XMPP handshake
    
                    try {
                        if(authenticatedLatch.await(xmppHandshakeTimeout, TimeUnit.MILLISECONDS)) {
                            // success, break out of connect loop
                            successfullyConnected = true;
                            break;
                        } else {
                            // attempt next
                            LOG.warn("XMPP handshake with {} at () timed out", otherServer, address.getAddress());
                        }
                    } catch (InterruptedException e) {
                        throw new RemoteServerTimeoutException("Connection to " + otherServer + " was interrupted", e);
                    }
                } 

                lastException = connectFuture.getException();
                LOG.warn("Failed connecting to XMPP server " + otherServer + " at " + address.getAddress(), connectFuture.getException());
                connector.dispose();
                connector = null;
            }
        } else {
            // should never happen
            throw new RemoteServerNotFoundException("DNS lookup of remote server failed");
        }
        
        if(!successfullyConnected) {
            String exceptionMsg = "Failed to connect to XMPP server at " + otherServer;
            
            if(lastException instanceof UnresolvedAddressException) {
                throw new RemoteServerNotFoundException(exceptionMsg);
            } else {
                throw new RemoteServerTimeoutException(exceptionMsg);
            }
            
        }
    }
    
    private NioSocketConnector createConnector(CountDownLatch authenticatedLatch) {
        NioSocketConnector connector = new NioSocketConnector();
        DefaultIoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
        filterChainBuilder.addLast("xmppCodec", new ProtocolCodecFilter(new XMPPProtocolCodecFactory()));
        filterChainBuilder.addLast("loggingFilter", new StanzaLoggingFilter());
        connector.setFilterChainBuilder(filterChainBuilder);
        connector.setHandler(new ConnectorIoHandler(authenticatedLatch));
        return connector;
    }


    
    private void startPinging() {
        pingTimer.schedule(new PingTask(), pingPeriod, pingPeriod);
    }
    
    /* (non-Javadoc)
     * @see org.apache.vysper.xmpp.server.s2s.XMPPServerConnector#write(org.apache.vysper.xmpp.stanza.Stanza)
     */
    public void write(Stanza stanza) {
        sessionContext.write(stanza);
    }

    public void close() {
        closed = true;
        if(!closed) {
            LOG.info("XMPP server connector to {} closing", otherServer);
            sessionContext.close();
            
            connector.dispose();
            pingTimer.cancel();
            LOG.info("XMPP server connector to {} closed", otherServer);
        }
    }

    public void pong() {
        // do nothing, all happy
    }

    public void timeout() {
        LOG.debug("XMPP server connector to {} timed out, closing", otherServer);
        close();
    }

    public boolean isClosed() {
        return closed;
    }

    private final class ConnectorIoHandler extends IoHandlerAdapter {
        private final CountDownLatch authenticatedLatch;

        private ConnectorIoHandler(CountDownLatch authenticatedLatch) {
            this.authenticatedLatch = authenticatedLatch;
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            LOG.info("Exception thrown by XMPP server connector to {}, probably a bug in Vysper", otherServer);
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            if(message == SslFilter.SESSION_SECURED) {
                // TODO handle unsecure
                // connection secured, send stream opener
                sessionStateHolder.setState(SessionState.ENCRYPTED);
                
                LOG.info("XMPP server connector to {} secured using TLS", otherServer);
                LOG.debug("XMPP server connector to {} restarting stream", otherServer);
                
                sessionContext.setIsReopeningXMLStream();
                
                Stanza opener = new ServerResponses().getStreamOpenerForServerConnector(serverRuntimeContext.getServerEnitity(), otherServer, XMPPVersion.VERSION_1_0, sessionContext);
                
                sessionContext.write(opener);
            } else {
                Stanza msg = (Stanza) message;
                
                if(msg.getName().equals("stream")) {
                    sessionContext.setSessionId(msg.getAttributeValue("id"));
                } else if(msg.getName().equals("features")) {
                    if(startTlsSupported(msg)) {
                        LOG.info("XMPP server connector to {} is starting TLS", otherServer);
                        Stanza startTlsStanza = new StanzaBuilder("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).build();
                        
                        sessionContext.write(startTlsStanza);
                        
                    } else if(dialbackSupported(msg)) {
                        Entity originating = serverRuntimeContext.getServerEnitity();
   
                        String dailbackId = new DailbackIdGenerator().generate(otherServer, originating, sessionContext.getSessionId());
                        
                        Stanza dbResult = new StanzaBuilder("result", NamespaceURIs.JABBER_SERVER_DIALBACK, "db")
                            .addAttribute("from", originating.getDomain())
                            .addAttribute("to", otherServer.getDomain())
                            .addText(dailbackId)
                            .build();
                        
                        sessionContext.write(dbResult);
                    } else {
                        throw new RuntimeException("Unsupported features");
                    }
                } else if(msg.getName().equals("result") && NamespaceURIs.JABBER_SERVER_DIALBACK.equals(msg.getNamespaceURI())) {
                    // TODO check and handle dailback result
                    sessionStateHolder.setState(SessionState.AUTHENTICATED);
                    
                    LOG.info("XMPP server connector to {} authenticated using dialback", otherServer);
                    authenticatedLatch.countDown();
                    
                    // connection established, start pinging
                    startPinging();
                } else if(msg.getName().equals("proceed") && NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS.equals(msg.getNamespaceURI())) {
                    sessionStateHolder.setState(SessionState.ENCRYPTION_STARTED);
                    
                    LOG.debug("XMPP server connector to {} switching to TLS", otherServer);
                    sessionContext.switchToTLS(false, true);
                } else {
                    // TODO other stanzas coming here?
                }
            }
        }

        private boolean startTlsSupported(Stanza stanza) {
            return !stanza.getInnerElementsNamed("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).isEmpty();
        }

        private boolean dialbackSupported(Stanza stanza) {
            // TODO check for dialback namespace
            return !stanza.getInnerElementsNamed("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).isEmpty();
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            // Socket was closed, make sure we close the connector
            LOG.info("XMPP server connector socket closed, closing connector");
            close();
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            sessionContext = new MinaBackedSessionContext(serverRuntimeContext, sessionStateHolder, session);
            sessionStateHolder.setState(SessionState.INITIATED);
            Stanza opener = new ServerResponses().getStreamOpenerForServerConnector(serverRuntimeContext.getServerEnitity(), otherServer, XMPPVersion.VERSION_1_0, sessionContext);
            
            sessionContext.write(opener);
        }
    }

    private class PingTask extends TimerTask {
        public void run() {
            XmppPingModule pingModule = serverRuntimeContext.getModule(XmppPingModule.class);
            if(pingModule != null) {
                pingModule.ping(DefaultXMPPServerConnector.this, serverRuntimeContext.getServerEnitity(), otherServer, pingTimeout, DefaultXMPPServerConnector.this);
            }
        }
    }
    

}
