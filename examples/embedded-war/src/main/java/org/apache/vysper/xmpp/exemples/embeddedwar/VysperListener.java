package org.apache.vysper.xmpp.exemples.embeddedwar;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.extension.websockets.XmppWebSocketServlet;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.server.XMPPServer;

public class VysperListener implements ServletContextListener {

    private XMPPServer server;

    public void contextInitialized(ServletContextEvent sce) {
        try {
            String domain = sce.getServletContext().getInitParameter("domain");
            
            StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
    
            final String adminJID = "admin@" + domain;
            final AccountManagement accountManagement = (AccountManagement) providerRegistry
                    .retrieve(AccountManagement.class);
    
            String initialPassword = System.getProperty("vysper.admin.initial.password", "CHOOSE SECURE PASSWORD");
                if (!accountManagement.verifyAccountExists(EntityImpl.parse(adminJID))) {
                    accountManagement.addUser(adminJID, initialPassword);
                }
    
            server = new XMPPServer(domain);
            server.addEndpoint(new TCPEndpoint());
            server.setStorageProviderRegistry(providerRegistry);
    
            server.setTLSCertificateInfo(sce.getServletContext().getResourceAsStream("WEB-INF/bogus_mina_tls.cert"), "password");
    
            try {
                server.start();
                System.out.println("vysper server is running...");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    
            server.addModule(new SoftwareVersionModule());
            server.addModule(new EntityTimeModule());
            server.addModule(new VcardTempModule());
            server.addModule(new XmppPingModule());
            
            sce.getServletContext().setAttribute(XmppWebSocketServlet.SERVER_RUNTIME_CONTEXT_ATTRIBUTE, server.getServerRuntimeContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        server.stop();
    }

}
