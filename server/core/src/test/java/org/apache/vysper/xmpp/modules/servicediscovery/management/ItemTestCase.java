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
package org.apache.vysper.xmpp.modules.servicediscovery.management;

import org.apache.vysper.StanzaAssert;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.junit.Test;


/**
 */
public class ItemTestCase {

    private static final Entity JID = EntityImpl.parseUnchecked("from@vysper.org");
    private static final String NAME = "foo";
    private static final String NODE = "bar";


    @Test
    public void constructorJid() {
        Item item = new Item(JID);
        
        Assert.assertEquals(JID, item.getJid());
        Assert.assertNull(item.getName());
        Assert.assertNull(item.getNode());
    }

    @Test
    public void constructorJidName() {
        Item item = new Item(JID, NAME);
        
        Assert.assertEquals(JID, item.getJid());
        Assert.assertEquals(NAME, item.getName());
        Assert.assertNull(item.getNode());
    }
    
    @Test
    public void constructorJidNameNode() {
        Item item = new Item(JID, NAME, NODE);
        
        Assert.assertEquals(JID, item.getJid());
        Assert.assertEquals(NAME, item.getName());
        Assert.assertEquals(NODE, item.getNode());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void constructorNullJid() {
        new Item(null);
    }

    @Test
    public void insertElement() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("test");
        
        Item item = new Item(JID, NAME, NODE);
        
        item.insertElement(stanzaBuilder);
        
        Stanza expected = new StanzaBuilder("test")
            .startInnerElement("item", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS)
            .addAttribute("jid", JID.getFullQualifiedName())
            .addAttribute("name", NAME)
            .addAttribute("node", NODE)
            .build();
        
        StanzaAssert.assertEquals(expected, stanzaBuilder.build());
        
    }
    
    @Test
    public void insertElementOptional() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("test");
        
        Item item = new Item(JID);
        
        item.insertElement(stanzaBuilder);
        
        Stanza expected = new StanzaBuilder("test")
            .startInnerElement("item", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS)
            .addAttribute("jid", JID.getFullQualifiedName())
            .build();
        
        StanzaAssert.assertEquals(expected, stanzaBuilder.build());
        
    }
    
}
