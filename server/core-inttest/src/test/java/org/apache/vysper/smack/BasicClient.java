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
package org.apache.vysper.smack;

import java.util.Random;

import org.apache.vysper.xmpp.authentication.Plain;
import org.apache.vysper.xmpp.cryptography.NonCheckingX509TrustManagerFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.core.SCRAMSHA1Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqversion.packet.Version;
import org.jivesoftware.smackx.time.packet.Time;
import org.jxmpp.jid.impl.JidCreate;

/**
 */
public class BasicClient {

    static class IQListener implements StanzaListener {

        public void processStanza(Stanza packet) {
            IQ iq = (IQ) packet;
            String iqString = iq.toString();
            System.out.println("T" + System.currentTimeMillis() + " IQ: " + iqString + ": " + iq.toXML(null));
        }
    }

    static class PresenceListener implements StanzaListener {

        public void processStanza(Stanza packet) {
            Presence presence = (Presence) packet;
            String iqString = presence.toString();
            final ExtensionElement extension = presence.getExtension("http://jabber.org/protocol/caps");
            if (extension != null)
                System.out
                        .println("T" + System.currentTimeMillis() + " Pres: " + iqString + ": " + presence.toXML(null));
        }
    }

    public static void main(String[] args) throws XMPPException {

        String me = args.length > 0 ? args[0] : "user1";
        String to = args.length < 2 ? null : args[1];

        try {
            XMPPTCPConnectionConfiguration connectionConfiguration = XMPPTCPConnectionConfiguration.builder()
                    .setHost("localhost").setCompressionEnabled(false)
                    .setCustomX509TrustManager(NonCheckingX509TrustManagerFactory.X509)
                    .setDebuggerFactory(ConsoleDebugger.Factory.INSTANCE)
                    .addEnabledSaslMechanism(SASLMechanism.PLAIN)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.required).build();

            XMPPTCPConnection connection = new XMPPTCPConnection(connectionConfiguration);
            connection.connect();

            connection.login(me + "@vysper.org", "password1");

            Roster.getInstanceFor(connection).setSubscriptionMode(Roster.SubscriptionMode.accept_all);

            connection.addSyncStanzaListener(new IQListener(), packet -> packet instanceof IQ);

            connection.addSyncStanzaListener(new PresenceListener(), packet -> packet instanceof Presence);

            Chat chat = null;
            if (to != null) {
                Presence presence = new Presence(Presence.Type.subscribe);
                presence.setFrom(connection.getUser());
                String toEntity = to + "@vysper.org";
                presence.setTo(toEntity);
                connection.sendStanza(presence);

                ChatManager chatManager = ChatManager.getInstanceFor(connection);
                chat = chatManager.chatWith(JidCreate.entityBareFrom(toEntity));
                chatManager.addIncomingListener((from, message, chat1) -> {
                    System.out.println("log received message: " + message.getBody());
                });
            }

            connection.sendStanza(new Presence(Presence.Type.available, "pommes", 1, Presence.Mode.available));

            Thread.sleep(1000);

            // query server version
            sendIQGetWithTimestamp(connection, new Version());

            // query server time
            sendIQGetWithTimestamp(connection, new Time());

            /*
             * while (to != null) { // chat.sendMessage("Hello " + to + " at " + new
             * Date()); try { Thread.sleep((new Random().nextInt(15)+1)*1000 ); } catch
             * (InterruptedException e) { ; } }
             */

            for (int i = 0; i < 10; i++) {
                connection.sendStanza(new Presence(Presence.Type.available, "pommes", 1, Presence.Mode.available));
                try {
                    Thread.sleep((new Random().nextInt(15) + 10) * 1000);
                } catch (InterruptedException e) {
                    ;
                }
                connection.sendStanza(new Presence(Presence.Type.available, "nickes", 1, Presence.Mode.away));
                try {
                    Thread.sleep((new Random().nextInt(15) + 10) * 1000);
                } catch (InterruptedException e) {
                    ;
                }
            }

            for (int i = 0; i < 2000; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    ;
                }
            }

            connection.disconnect();
        } catch (Throwable e) {
            try {
                Thread.sleep(120 * 1000);
            } catch (InterruptedException ie) {
                ;
            }
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        }
        System.exit(0);
    }

    private static void sendIQGetWithTimestamp(XMPPConnection connection, IQ iq)
            throws SmackException.NotConnectedException, InterruptedException {
        iq.setType(IQ.Type.get);
        connection.sendStanza(iq);
        System.out.println("T" + System.currentTimeMillis() + " IQ request sent");
    }
}
