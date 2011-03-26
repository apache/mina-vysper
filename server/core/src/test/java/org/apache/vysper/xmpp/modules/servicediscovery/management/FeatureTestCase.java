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
public class FeatureTestCase {

    private static final String VAR = "foo";


    @Test
    public void constructorVar() {
        Feature feature = new Feature(VAR);
        
        Assert.assertEquals(VAR, feature.getVar());
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorNullJid() {
        new Feature(null);
    }

    @Test
    public void insertElement() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("test");
        
        Feature feature = new Feature(VAR);
        
        feature.insertElement(stanzaBuilder);
        
        Stanza expected = new StanzaBuilder("test")
            .startInnerElement("feature", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO)
            .addAttribute("var", VAR)
            .build();
        
        StanzaAssert.assertEquals(expected, stanzaBuilder.build());
    }
}
