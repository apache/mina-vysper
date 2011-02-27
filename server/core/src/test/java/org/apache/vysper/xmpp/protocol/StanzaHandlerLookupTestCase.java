/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.vysper.xmpp.protocol;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.BaseStreamStanzaDictionary;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.MessageHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.TestIQHandler;
import org.apache.vysper.xmpp.modules.core.im.handler.PresenceHandler;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class StanzaHandlerLookupTestCase extends TestCase {

    private static final Entity SERVER_ENTITY = EntityImpl.parseUnchecked("vysper.org");

    private static final Entity SUBDOMAIN_ENTITY = EntityImpl.parseUnchecked("sub.vysper.org");

    public void testDictionaryHierarchy() {
        NamespaceHandlerDictionary upperNamespaceHandlerDictionary = new NamespaceHandlerDictionary("testNSURI1");
        CallTestStanzaHandler upperStanzaHandler = new CallTestStanzaHandler("testDictionaryHierarchy", "testNSURI1");
        upperNamespaceHandlerDictionary.register(upperStanzaHandler);

        NamespaceHandlerDictionary lowerNamespaceHandlerDictionary = new NamespaceHandlerDictionary("testNSURI2");
        CallTestStanzaHandler lowerStanzaHandler = new CallTestStanzaHandler("testDictionaryHierarchy", "testNSURI2");
        lowerNamespaceHandlerDictionary.register(lowerStanzaHandler);

        StanzaHandlerLookup stanzaHandlerLookup = initStanzaHandlerLookup();
        stanzaHandlerLookup.addDictionary(upperNamespaceHandlerDictionary);
        stanzaHandlerLookup.addDictionary(lowerNamespaceHandlerDictionary);

        Stanza nonExistingStanza = new Stanza("testNSURI", "testDictionaryHierarchyNotExist", null,
                new ArrayList<Attribute>(), new ArrayList<XMLFragment>());
        StanzaHandler handler = stanzaHandlerLookup.getHandler(nonExistingStanza);
        assertNull("handler not found", handler);

        Stanza existingStanzaNS1 = new Stanza("testNSURI1", "testDictionaryHierarchy", null,
                new ArrayList<Attribute>(), new ArrayList<XMLFragment>());
        handler = stanzaHandlerLookup.getHandler(existingStanzaNS1);
        assertNotNull("handler found in dict1", handler);
        assertTrue("verify got called", ((CallTestStanzaHandler) handler).isVerifyCalled());
        assertNotSame("lower not found", lowerStanzaHandler, handler);
        assertSame("upper found", upperStanzaHandler, handler);

        Stanza existingStanzaNS2 = new Stanza("testNSURI2", "testDictionaryHierarchy", null,
                new ArrayList<Attribute>(), new ArrayList<XMLFragment>());
        handler = stanzaHandlerLookup.getHandler(existingStanzaNS2);
        assertTrue("verify got called", ((CallTestStanzaHandler) handler).isVerifyCalled());
        assertNotNull("handler found in dict2", handler);
        assertSame("lower found", lowerStanzaHandler, handler);
        assertNotSame("upper not found", upperStanzaHandler, handler);

    }

    public void testLookupCoreHandlerClientNS() {
        StanzaHandlerLookup stanzaHandlerLookup = initStanzaHandlerLookup();
        stanzaHandlerLookup.addDictionary(new BaseStreamStanzaDictionary());

        Stanza stanza = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT).build();
        StanzaHandler handler = stanzaHandlerLookup.getHandler(stanza);

        assertNotNull("handler found", handler);
        assertTrue("iq handler found", handler instanceof IQHandler);

    }

    private StanzaHandlerLookup initStanzaHandlerLookup() {
        return new StanzaHandlerLookup(null);
    }

    public void testLookupCoreHandlerWrongNamespace() {
        StanzaHandlerLookup stanzaHandlerLookup = initStanzaHandlerLookup();
        stanzaHandlerLookup.addDictionary(new BaseStreamStanzaDictionary());

        Stanza stanza = new StanzaBuilder("iq", "arbitraryNamespace").build();
        StanzaHandler handler = stanzaHandlerLookup.getHandler(stanza);

        assertTrue("iq handler with arbitrary namespace not found", handler instanceof ServiceUnavailableStanzaErrorHandler);
    }

    public void testLookupPresenceHandler() {
        StanzaHandlerLookup stanzaHandlerLookup = initStanzaHandlerLookup();
        stanzaHandlerLookup.addDictionary(new BaseStreamStanzaDictionary());

        Stanza stanza = new StanzaBuilder("presence", NamespaceURIs.JABBER_CLIENT).build();
        StanzaHandler handler = stanzaHandlerLookup.getHandler(stanza);

        assertNotNull("handler found", handler);
        assertTrue("iq handler found", handler instanceof PresenceHandler);
    }

    public void testLookupMessageHandler() {
        StanzaHandlerLookup stanzaHandlerLookup = initStanzaHandlerLookup();
        stanzaHandlerLookup.addDictionary(new BaseStreamStanzaDictionary());

        Stanza stanza = new StanzaBuilder("message", NamespaceURIs.JABBER_CLIENT).build();
        StanzaHandler handler = stanzaHandlerLookup.getHandler(stanza);

        assertNotNull("handler found", handler);
        assertTrue("iq handler found", handler instanceof MessageHandler);
    }

    public void testLookupSpecializedIQHandler() {
        StanzaHandlerLookup stanzaHandlerLookup = initStanzaHandlerLookup();
        stanzaHandlerLookup.addDictionary(new BaseStreamStanzaDictionary());

        NamespaceHandlerDictionary testDictionary = new NamespaceHandlerDictionary("test:namespace:OK");
        testDictionary.register(new TestIQHandler("testOK", "test:namespace:OK"));
        stanzaHandlerLookup.addDictionary(testDictionary);

        Stanza stanza = buildStanza("testOK", "test:namespace:FAIL");
        StanzaHandler handler = stanzaHandlerLookup.getHandler(stanza);
        assertTrue("handler not found for NS", handler instanceof ServiceUnavailableStanzaErrorHandler);

        stanza = buildStanza("testFAIL", "test:namespace:OK");
        handler = stanzaHandlerLookup.getHandler(stanza);
        assertTrue("handler not found for name", handler instanceof ServiceUnavailableStanzaErrorHandler);

        stanza = buildStanza("testOK", "test:namespace:OK");
        handler = stanzaHandlerLookup.getHandler(stanza);
        assertNotNull("handler found", handler);
        assertTrue("test handler", TestIQHandler.class.equals(handler.getClass()));
    }

    private Stanza buildStanza(String name, String namespaceURI) {
        return buildStanza(name, namespaceURI, null);
    }

    private Stanza buildStanza(String name, String namespaceURI, String to) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("iq", NamespaceURIs.JABBER_CLIENT);
        stanzaBuilder.addAttribute("id", "1");
        stanzaBuilder.addAttribute("type", "get");
        if (to != null) {
            stanzaBuilder.addAttribute("to", to);
        }
        stanzaBuilder.startInnerElement(name, namespaceURI).endInnerElement();
        Stanza stanza = stanzaBuilder.build();
        return stanza;
    }
}
