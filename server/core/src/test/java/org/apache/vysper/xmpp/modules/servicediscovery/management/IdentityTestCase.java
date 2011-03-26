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
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.junit.Test;


/**
 */
public class IdentityTestCase {

    private static final String CATEGORY = "foo";
    private static final String TYPE = "bar";
    private static final String NAME = "fez";


    @Test
    public void constructorCategoryType() {
        Identity identity = new Identity(CATEGORY, TYPE);
        
        Assert.assertEquals(CATEGORY, identity.getCategory());
        Assert.assertEquals(TYPE, identity.getType());
        Assert.assertNull(identity.getName());
    }

    @Test
    public void constructorCategoryTypeName() {
        Identity identity = new Identity(CATEGORY, TYPE, NAME);
        
        Assert.assertEquals(CATEGORY, identity.getCategory());
        Assert.assertEquals(TYPE, identity.getType());
        Assert.assertEquals(NAME, identity.getName());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void constructorNullCategory() {
        new Identity(null, TYPE);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorNullType() {
        new Identity(CATEGORY, null);
    }
    
    @Test
    public void insertElement() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("test");
        
        Identity identity = new Identity(CATEGORY, TYPE, NAME);
        identity.insertElement(stanzaBuilder);
        
        Stanza expected = new StanzaBuilder("test")
            .startInnerElement("identity", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO)
            .addAttribute("category", CATEGORY)
            .addAttribute("type", TYPE)
            .addAttribute("name", NAME)
            .build();
        
        StanzaAssert.assertEquals(expected, stanzaBuilder.build());
    }

    @Test
    public void insertElementOptional() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("test");
        
        Identity identity = new Identity(CATEGORY, TYPE);
        identity.insertElement(stanzaBuilder);
        
        Stanza expected = new StanzaBuilder("test")
        .startInnerElement("identity", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO)
        .addAttribute("category", CATEGORY)
        .addAttribute("type", TYPE)
        .build();
        
        StanzaAssert.assertEquals(expected, stanzaBuilder.build());
    }
}
