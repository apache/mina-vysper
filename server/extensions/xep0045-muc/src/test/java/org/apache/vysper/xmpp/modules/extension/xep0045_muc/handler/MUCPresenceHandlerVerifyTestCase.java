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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCFeatures;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class MUCPresenceHandlerVerifyTestCase extends TestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("user@vysper.org");

    private static final Entity TO = EntityImpl.parseUnchecked("room@chat.vysper.org");

    private MUCPresenceHandler presenceHandler;

    @Override
    protected void setUp() throws Exception {
        Conference conference = new Conference("foo", new MUCFeatures());
        presenceHandler = new MUCPresenceHandler(conference);
    }

    public void testVerifyNonPresence() {
        StanzaBuilder builder = StanzaBuilder.createMessageStanza(FROM, TO, "en", "foo");
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC);
        builder.endInnerElement();

        assertFalse(presenceHandler.verify(builder.build()));
    }

    public void testVerifyWithMUCNamespace() {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(FROM, TO, null, null, null, null);
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC);
        builder.endInnerElement();

        assertTrue(presenceHandler.verify(builder.build()));
    }

    public void testVerifyWithoutMUCNamespace() {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(FROM, TO, null, null, null, null);

        assertTrue(presenceHandler.verify(builder.build()));
    }

    public void testVerifyWithoutMUCNamespaceInnerElement() {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(FROM, TO, null, null, null, null);
        builder.startInnerElement("foo").endInnerElement();

        assertTrue(presenceHandler.verify(builder.build()));
    }
}
