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
package org.apache.vysper.xmpp.stanza;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 */
public class StanzaBuilderTestCase extends TestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");
    private static final Entity TO = EntityImpl.parseUnchecked("to@vysper.org");
    private static final String LANG = "en";
    private static final String BODY = "Hello world";

    
    public void testInnerElementNamespace() {
        StanzaBuilder builder = new StanzaBuilder("iq");
        builder.startInnerElement("foo", "urn:bar");
        builder.endInnerElement();

        Stanza stanza = builder.build();

        XMLElement innerElm = stanza.getFirstInnerElement();

        assertEquals("foo", innerElm.getName());
        assertEquals("urn:bar", innerElm.getNamespaceURI());

    }
    
    public void testRewriteNamespace() throws XMLSemanticError {
        Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY)
            .startInnerElement("foo", "http://someothernamespace")
            .startInnerElement("bar", NamespaceURIs.JABBER_CLIENT)
            .addAttribute("inner", "attribute")
            .addText("inner text")
            .endInnerElement()
            .endInnerElement()
            .addText("some text")
            .build();
        
        Stanza rewritten = StanzaBuilder.rewriteNamespace(stanza, NamespaceURIs.JABBER_CLIENT, NamespaceURIs.JABBER_SERVER);

        System.out.println(new Renderer(rewritten).getComplete());
        Assert.assertEquals("message", rewritten.getName());
        Assert.assertEquals(NamespaceURIs.JABBER_SERVER, rewritten.getNamespaceURI());
        Assert.assertEquals(FROM, rewritten.getFrom());
        Assert.assertEquals(TO, rewritten.getTo());
        
        XMLElement body = rewritten.getSingleInnerElementsNamed("body", NamespaceURIs.JABBER_SERVER);
        Assert.assertNotNull(body);
        Assert.assertEquals(BODY, body.getInnerText().getText());
        
        XMLElement foo = rewritten.getSingleInnerElementsNamed("foo", "http://someothernamespace");
        Assert.assertNotNull(foo);
        
        // wrapped elements must not be rewritten
        XMLElement bar = foo.getSingleInnerElementsNamed("bar", NamespaceURIs.JABBER_CLIENT);
        Assert.assertNotNull(bar);
        
        Assert.assertEquals("attribute", bar.getAttributeValue("inner"));
        Assert.assertEquals("inner text", bar.getInnerText().getText());
    }
}
