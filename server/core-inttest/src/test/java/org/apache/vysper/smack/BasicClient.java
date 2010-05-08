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

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.Version;
import org.jivesoftware.smackx.packet.Time;

import java.util.Random;

/**
 */
public class BasicClient {

    static class IQListener implements PacketListener {

        public void processPacket(Packet packet) {
            IQ iq = (IQ) packet;
            String iqString = iq.toString();
            System.out.println("T" + System.currentTimeMillis() + " IQ: " + iqString + ": " + iq.toXML());
        }
    }

    static class PresenceListener implements PacketListener {

        public void processPacket(Packet packet) {
            Presence presence = (Presence) packet;
            String iqString = presence.toString();
            final PacketExtension extension = presence.getExtension("http://jabber.org/protocol/caps");
            if (extension != null) System.out.println("T" + System.currentTimeMillis() + " Pres: " + iqString + ": " + presence.toXML());
        }
    }

    public static void main(String[] args) throws XMPPException {

        String me = args.length > 0 ? args[0] : "user1";
        String to = args.length < 2 ? null : args[1];
        
        try {
            ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration("localhost");
//            ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration("xmpp.eu");
            connectionConfiguration.setCompressionEnabled(false);
            connectionConfiguration.setSelfSignedCertificateEnabled(true);
            connectionConfiguration.setExpiredCertificatesCheckEnabled(false);
            connectionConfiguration.setDebuggerEnabled(true);
            connectionConfiguration.setSASLAuthenticationEnabled(true);
            connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
            XMPPConnection.DEBUG_ENABLED = true;
            XMPPConnection connection = new XMPPConnection(connectionConfiguration);
            connection.connect();

            SASLAuthentication saslAuthentication = connection.getSASLAuthentication();
//            saslAuthentication.authenticateAnonymously();
//            saslAuthentication.authenticate("user1@vysper.org", "password1", "test");

//            if (!saslAuthentication.isAuthenticated()) return;


            connection.login(me + "@vysper.org", "password1");

            connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);

            connection.addPacketListener(new IQListener() , new PacketFilter() {
                public boolean accept(Packet packet) {
                    return packet instanceof IQ;
                }
            });

            connection.addPacketListener(new PresenceListener() , new PacketFilter() {
                public boolean accept(Packet packet) {
                    return packet instanceof Presence;
                }
            });

            Chat chat = null;
            if (to != null) {
                Presence presence = new Presence(Presence.Type.subscribe);
                presence.setFrom(connection.getUser());
                String toEntity = to + "@vysper.org";
                presence.setTo(toEntity);
                connection.sendPacket(presence);
    
                    chat = connection.getChatManager().createChat(toEntity, new MessageListener() {
                    public void processMessage(Chat inchat, Message message) {
                        System.out.println("log received message: " + message.getBody());
                       }
                     });
            }
            
            connection.sendPacket(new Presence(Presence.Type.available, "pommes", 1, Presence.Mode.available));

            Thread.sleep(1000);
            
            // query server version
            sendIQGetWithTimestamp(connection, new Version());

            // query server time
            sendIQGetWithTimestamp(connection, new Time());

/*            while (to != null) {
//                chat.sendMessage("Hello " + to + " at " + new Date());
                try { Thread.sleep((new Random().nextInt(15)+1)*1000 ); } catch (InterruptedException e) { ; }
            }*/

            for (int i = 0; i < 10; i++) {
                connection.sendPacket(new Presence(Presence.Type.available, "pommes", 1, Presence.Mode.available));
                try { Thread.sleep((new Random().nextInt(15)+10)*1000 ); } catch (InterruptedException e) { ; }
                connection.sendPacket(new Presence(Presence.Type.available, "nickes", 1, Presence.Mode.away));
                try { Thread.sleep((new Random().nextInt(15)+10)*1000 ); } catch (InterruptedException e) { ; }
            }

            for (int i = 0; i < 2000; i++) {
                try { Thread.sleep(500); } catch (InterruptedException e) { ; }
            }
            
            connection.disconnect(); 
        } catch (Throwable e) {
            try { Thread.sleep(120*1000 ); } catch (InterruptedException ie) { ; }
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.exit(0);
    }

    private static void sendIQGetWithTimestamp(XMPPConnection connection, IQ iq) {
        iq.setType(IQ.Type.GET);
        connection.sendPacket(iq);
        System.out.println("T" + System.currentTimeMillis() + " IQ request sent");
    }
}
