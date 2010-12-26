package org.apache.vysper.xmpp.server.s2s;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.vysper.mina.MinaBackedSessionContext;
import org.apache.vysper.mina.StanzaLoggingFilter;
import org.apache.vysper.mina.codec.XMPPProtocolCodecFactory;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DailbackIdGenerator;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

public class XMPPServerConnector {

    private ServerRuntimeContext serverRuntimeContext;
    private MinaBackedSessionContext sessionContext;
    private Entity otherServer;
    private SessionStateHolder sessionStateHolder = new SessionStateHolder();
    private IoConnector connector = new NioSocketConnector();
    
    public XMPPServerConnector(Entity otherServer, ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
        this.otherServer = otherServer;

        DefaultIoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
        filterChainBuilder.addLast("xmppCodec", new ProtocolCodecFilter(new XMPPProtocolCodecFactory()));
        filterChainBuilder.addLast("loggingFilter", new StanzaLoggingFilter());
        connector.setFilterChainBuilder(filterChainBuilder);
    }

    public void start() {
        final CountDownLatch latch = new CountDownLatch(1);
        
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                cause.printStackTrace();
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                Stanza msg = (Stanza) message;
                
                if(msg.getName().equals("stream")) {
                    sessionContext.setSessionId(msg.getAttributeValue("id"));
                } else if(msg.getName().equals("features")) {
                    if(dialbackSupported(msg)) {
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
                    System.out.println("Done with dialback");
                    latch.countDown();
                } else {
                    // TODO other stanzas coming here?
                }
            }
            
            private boolean dialbackSupported(Stanza stanza) {
                // TODO check for dialback namespace
                return !stanza.getInnerElementsNamed("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).isEmpty();
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                System.out.println("Closed");
            }

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                sessionContext = new MinaBackedSessionContext(serverRuntimeContext, sessionStateHolder, session);
                sessionStateHolder.setState(SessionState.INITIATED);
                Stanza opener = new ServerResponses().getStreamOpenerForServerConnector(serverRuntimeContext.getServerEnitity(), otherServer, XMPPVersion.VERSION_1_0, sessionContext);
                
                sessionContext.write(opener);
            }
        });
        
        XmppEndpointResolver resolver = new XmppEndpointResolver();
        InetSocketAddress address = resolver.resolveXmppServer(otherServer.getDomain()).get(0).getAddress();
        connector.connect(address);
        
        // make this method sync
        // TODO handle timeout
        try {
            latch.await(20000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO handle
        }
    }
    
    public void stop() {
        connector.dispose();
    }
    
    public void write(Stanza stanza) {
        sessionContext.write(stanza);
    }
}
