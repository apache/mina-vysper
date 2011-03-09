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

import java.util.Arrays;
import java.util.Collection;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.junit.Test;

public class MemoryOfflineStorageProviderTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");
    private static final Entity TO = EntityImpl.parseUnchecked("to@vysper.org");
    
    private MemoryOfflineStorageProvider provider = new MemoryOfflineStorageProvider();
    
    @Test
    public void storeAndGet() {
        Stanza stanza1 = StanzaBuilder.createMessageStanza(FROM, TO, null, "body").build();
        provider.receive(stanza1);
        
        Stanza stanza2 = StanzaBuilder.createPresenceStanza(FROM, TO, null, PresenceStanzaType.SUBSCRIBE, null, null).build();
        provider.receive(stanza2);
        
        // Presence errors should not be stored
        Stanza stanza3 = StanzaBuilder.createPresenceStanza(FROM, TO, null, PresenceStanzaType.ERROR, null, null).build();
        provider.receive(stanza3);
        
        Collection<Stanza> stanzas = provider.getStanzasFor(TO);
        
        Assert.assertEquals(Arrays.asList(stanza1, stanza2), stanzas);
    }

    @Test
    public void getWithNothingStored() {
        Collection<Stanza> stanzas = provider.getStanzasFor(TO);
        
        Assert.assertEquals(0, stanzas.size());
    }

    
}
