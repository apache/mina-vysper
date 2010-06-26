package org.apache.vysper.demo.pubsub.client;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.FormType;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

public class PubsubHelloWorld {
    public static void main(String[] args) {
        PubsubHelloWorld pshw = new PubsubHelloWorld();

        try {
            pshw.run();
        } catch (XMPPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void run() throws XMPPException {
        ConfigureForm form = new ConfigureForm(FormType.submit);
        form.setPersistentItems(false);
        form.setDeliverPayloads(true);
        form.setAccessModel(AccessModel.open);

        PubSubManager manager = new PubSubManager(createConnection(), "pubsub.vysper.org");
        Node myNode = manager.createNode("TestNode", form);

        SimplePayload payload = new SimplePayload("book", "pubsub:test:book",
                "<book xmlns='pubsub:test:book'><title>Lord of the Rings</title></book>");

        Item item = new Item(payload.getElementName()); // , payload);

        // Required to recieve the events being published
        myNode.addItemEventListener(getMyEventHandler());

        // Publish item
        ((LeafNode) myNode).publish(item);

        try {
            Thread.currentThread().sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private ItemEventListener getMyEventHandler() {
        // TODO Auto-generated method stub
        return new ItemEventListener<Item>() {

            public void handlePublishedItems(ItemPublishEvent<Item> arg0) {
                System.out.println(arg0);

            }
        };
    }

    private XMPPConnection createConnection() throws XMPPException {
        // Create a connection to the XMPP server.
        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration("vysper.org");
        connectionConfiguration.setCompressionEnabled(false);
        connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
        connectionConfiguration.setSASLAuthenticationEnabled(true);
        connectionConfiguration.setDebuggerEnabled(false);
        connectionConfiguration.setKeystorePath("../../core/src/main/config/bogus_mina_tls.cert");
        connectionConfiguration.setTruststorePath("../../core/src/main/config/bogus_mina_tls.cert");
        connectionConfiguration.setTruststorePassword("boguspw");

        XMPPConnection con = new XMPPConnection(connectionConfiguration);

        con.getSASLAuthentication().supportSASLMechanism("PLAIN", 0);
        con.connect();

        // Most servers require you to login before performing other tasks.
        con.login("user1@vysper.org", "password1");

        return con;
    }
}