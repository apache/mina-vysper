package org.apache.vysper.xmpp.server.s2s;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class S2SIntegrationTestCase extends TestCase {

    public static Test suite() {
        File testsDir = new File(".");
        File[] testFiles = testsDir.listFiles(new FilenameFilter() {
            public boolean accept(File arg0, String name) {
                return name.startsWith("s2s-");
            }
        });
        
        TestSuite suite = new TestSuite("S2S integration tests");
        for(File testFile : testFiles) {
            try {
                suite.addTest(new S2SIntegrationTestCase(testFile));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
        return suite;
    }
    
    private File testFile;
    private Properties config = new Properties();
    
    public S2SIntegrationTestCase(File testFile) throws IOException {
        this.testFile = testFile;
        config.load(new FileInputStream(testFile));
    }

    @Override
    public String getName() {
        return testFile.getName();
    }

    protected void runTest() throws Throwable {
        Entity localServer = EntityImpl.parseUnchecked(config.getProperty("local.server"));
        String localConnect = config.getProperty("local.connect", localServer.getFullQualifiedName());
        Entity localUser = EntityImpl.parseUnchecked(config.getProperty("local.user"));
        String localPassword = "password";
        Entity remoteServer = EntityImpl.parseUnchecked(config.getProperty("remote.server"));
        Entity remoteUser = EntityImpl.parseUnchecked(config.getProperty("remote.user"));
        String remotePassword = config.getProperty("remote.password");

        String keystorePath = config.getProperty("keystore.path");
        String keystorePassword = config.getProperty("keystore.password");
        
        XMPPServer server = createLocalServer(localServer, localUser, localPassword, keystorePath, keystorePassword);
        
        Thread.sleep(2000);

        System.out.println();
        System.out.println();
        System.out.println("Connecting local client");
        System.out.println();
        System.out.println();
        
        LinkedBlockingQueue<Packet> localClientPackages = new LinkedBlockingQueue<Packet>();
        LinkedBlockingQueue<Packet> remoteClientPackages = new LinkedBlockingQueue<Packet>();
        
        XMPPConnection localClient = connectClient(localConnect, localUser, localPassword, keystorePath, keystorePassword, localClientPackages);

        System.out.println();
        System.out.println();
        System.out.println("Connecting remote client");
        System.out.println();
        System.out.println();

        XMPPConnection remoteClient = connectClient(remoteServer.getDomain(), remoteUser, remotePassword, keystorePath, keystorePassword, remoteClientPackages);

        Thread.sleep(3000);

        System.out.println();
        System.out.println();
        System.out.println("Sending message from local to remote");
        System.out.println();
        System.out.println();

        remoteClientPackages.clear();
        Message msg = new Message(remoteUser.getFullQualifiedName());
        msg.setBody("Hello world");
        
        localClient.sendPacket(msg);
        
        Packet packet = remoteClientPackages.poll(15000, TimeUnit.MILLISECONDS);
        if(packet != null && packet instanceof Message) {
            System.out.println("!!!!!!" + ((Message)packet).getBody());
        } else {
            fail("Message not received by remote client");
        }

        Thread.sleep(3000);

        System.out.println();
        System.out.println();
        System.out.println("Sending message from remote to local");
        System.out.println();
        System.out.println();

        localClientPackages.clear();
        msg = new Message(localUser.getFullQualifiedName());
        msg.setBody("Hello world");
        
        remoteClient.sendPacket(msg);
        
        packet = localClientPackages.poll(15000, TimeUnit.MILLISECONDS);
        if(packet != null && packet instanceof Message) {
            System.out.println("!!!!!!" + ((Message)packet).getBody());
        } else {
            fail("Message not received by local client");
        }
        
        Thread.sleep(15000);
        System.out.println();
        System.out.println();
        System.out.println("Closing down");
        System.out.println();
        System.out.println();

        remoteClient.disconnect();
        localClient.disconnect();
        
        Thread.sleep(5000);
        
        server.stop();
    }

    private XMPPConnection connectClient(String host, Entity user, String password, String keystorePath, String keystorePassword, final LinkedBlockingQueue<Packet> packageQueue)
            throws XMPPException {
        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(host, 5222);
        connectionConfiguration.setKeystorePath(keystorePath);
        connectionConfiguration.setTruststorePath(keystorePath);
        connectionConfiguration.setTruststorePassword(keystorePassword);
        XMPPConnection client = new XMPPConnection(connectionConfiguration);

        client.connect();
        client.login(user.getNode(), password);
        client.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                System.out.println("# " + packet);
                packageQueue.add(packet);
            }
        }, new PacketFilter() {
            public boolean accept(Packet arg0) {
                return true;
            }
        });

        
        return client;
    }

    private XMPPServer createLocalServer(Entity localServer, Entity localUser, String password, String keystorePath,
            String keystorePassword) throws AccountCreationException, FileNotFoundException, Exception {
        XMPPServer server = new XMPPServer(localServer.getDomain());

        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
        final AccountManagement accountManagement = (AccountManagement) providerRegistry
        .retrieve(AccountManagement.class);

        if (!accountManagement.verifyAccountExists(localUser)) {
            accountManagement.addUser(localUser.getFullQualifiedName(), password);
        }

        // S2S endpoint
        TCPEndpoint s2sEndpoint = new TCPEndpoint();
        s2sEndpoint.setPort(5269);
        server.addEndpoint(s2sEndpoint);
        
        // C2S endpoint
        server.addEndpoint(new TCPEndpoint());
        
        server.setStorageProviderRegistry(providerRegistry);
        server.setTLSCertificateInfo(new File(keystorePath), keystorePassword);
        
        server.start();
        return server;
    }
}
