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
package org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage;

import java.util.Collection;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.junit.Test;


/**
 */
public class AbstractOfflineStorageProviderTestCase {

    private static class TestOfflineStorageProvider extends AbstractOfflineStorageProvider {
        public Stanza storedStanza;
        
        public Collection<Stanza> getStanzasFor(Entity jid) {
            return null;
        }
        
        @Override
        protected void storeStanza(Stanza stanza) {
            this.storedStanza = stanza;
        }
    }
    
    @Test
    public void message() {
        assertStored((MessageStanzaType)null);
        assertStored(MessageStanzaType.CHAT);
        assertNotStored(MessageStanzaType.GROUPCHAT);
        assertNotStored(MessageStanzaType.HEADLINE);
        assertNotStored(MessageStanzaType.ERROR);
    }

    @Test
    public void presence() {
        assertStored(PresenceStanzaType.SUBSCRIBE);
        assertStored(PresenceStanzaType.SUBSCRIBED);
        assertStored(PresenceStanzaType.UNSUBSCRIBE);
        assertStored(PresenceStanzaType.UNSUBSCRIBED);
        assertNotStored(PresenceStanzaType.ERROR);
        assertNotStored(PresenceStanzaType.PROBE);
        assertNotStored(PresenceStanzaType.UNAVAILABLE);
    }
    
    private void assertStored(MessageStanzaType type) {
        assertStored("message", type != null ? type.value() : null, true);
    }
    
    private void assertNotStored(MessageStanzaType type) {
        assertStored("message", type != null ? type.value() : null, false);
    }
    
    private void assertStored(PresenceStanzaType type) {
        assertStored("presence", type.value(), true);
    }
    
    private void assertNotStored(PresenceStanzaType type) {
        assertStored("presence", type.value(), false);
    }
    
    private void assertStored(String stanzaName, String type, boolean stored) {
        TestOfflineStorageProvider provider = new TestOfflineStorageProvider();
        
        StanzaBuilder stanzaBuilder = new StanzaBuilder(stanzaName, NamespaceURIs.JABBER_CLIENT);
        if(type != null) {
            stanzaBuilder.addAttribute("type", type);
        }
        Stanza stanza = stanzaBuilder.build();
    
        provider.receive(stanza);
    
        if(stored) Assert.assertNotNull("Stanza not stored, but must be", provider.storedStanza);
        else Assert.assertNull("Stanza stored, but must not be", provider.storedStanza);

    }
}
