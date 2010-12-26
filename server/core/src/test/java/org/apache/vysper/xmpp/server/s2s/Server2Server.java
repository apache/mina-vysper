package org.apache.vysper.xmpp.server.s2s;
import java.io.File;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.apache.vysper.xmpp.server.s2s.XMPPServerConnector;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;


public class Server2Server {

    // TODO temporarily uses hard coded servers, change for your own tests
    private static Entity localServer = EntityImpl.parseUnchecked("protocol7.dyndns.org");
    private static Entity localUser = EntityImpl.parseUnchecked("niklas@protocol7.dyndns.org");
    private static Entity remoteServer = EntityImpl.parseUnchecked("jabber.org");
    private static Entity remoteUser = EntityImpl.parseUnchecked("niklas@protocol7.com");
    
    public static void main(String[] args) throws Exception {
        
        XMPPServer server = new XMPPServer(localServer.getDomain());

        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
        final AccountManagement accountManagement = (AccountManagement) providerRegistry
        .retrieve(AccountManagement.class);

        if (!accountManagement.verifyAccountExists(localUser)) {
            accountManagement.addUser(localUser.getFullQualifiedName(), "password1");
        }

        // S2S endpoint
        TCPEndpoint s2sEndpoint = new TCPEndpoint();
        s2sEndpoint.setPort(5269);
        server.addEndpoint(s2sEndpoint);
        
        // C2S endpoint
        server.addEndpoint(new TCPEndpoint());
        server.setStorageProviderRegistry(providerRegistry);
        server.setTLSCertificateInfo(new File("src/main/config/bogus_mina_tls.cert"), "boguspw");
        server.start();
        ServerRuntimeContext serverRuntimeContext = server.getServerRuntimeContext();
        
        Thread.sleep(2000);

        XMPPServerConnectorRegistry registry = serverRuntimeContext.getServerConnectorRegistry();
        
        XMPPServerConnector connector = registry.getConnector(remoteServer);
        
        Stanza ping = new StanzaBuilder("iq", NamespaceURIs.JABBER_SERVER)
            .addAttribute("from", serverRuntimeContext.getServerEnitity().getDomain())
            .addAttribute("to", remoteServer.getDomain())
            .addAttribute("type", "get")
            .addAttribute("id", "123")
            .startInnerElement("ping", NamespaceURIs.URN_XMPP_PING).endInnerElement().build();
        connector.write(ping);

//        Stanza stanza = new StanzaBuilder("message", NamespaceURIs.JABBER_SERVER)
//            .addAttribute("from", localUser.getFullQualifiedName())
//            .addAttribute("to", remoteUser.getFullQualifiedName())
//            .startInnerElement("body", NamespaceURIs.JABBER_SERVER)
//            .addText("Hello world")
//            .endInnerElement()
//            .build();
            
//        connector.write(stanza);
        
        
        Thread.sleep(2000);
        
        connector.stop();
        server.stop();
    }
    
}
