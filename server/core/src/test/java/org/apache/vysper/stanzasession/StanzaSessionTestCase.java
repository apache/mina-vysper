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
package org.apache.vysper.stanzasession;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaRelayBroker;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.ProtocolWorker;
import org.apache.vysper.xmpp.protocol.SimpleStanzaHandlerExecutorFactory;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.server.components.SimpleComponentRegistry;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.DefaultResourceRegistry;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;

import junit.framework.TestCase;

/**
 */
public class StanzaSessionTestCase extends TestCase {

    private StanzaSessionFactory sessionFactory;

    @Override
    protected void setUp() throws Exception {
        StanzaRelayBroker relay = new StanzaRelayBroker();

        List<HandlerDictionary> dictionaries = new ArrayList<HandlerDictionary>();
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.base.BaseStreamStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.starttls.StartTLSStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.sasl.SASLStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.bind.BindResourceDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.session.SessionStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.compatibility.jabber_iq_auth.JabberIQAuthDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.roster.RosterDictionary());

        Entity serverEntity = new EntityImpl(null, "test", null);
        ProtocolWorker protocolWorker = new ProtocolWorker(new SimpleStanzaHandlerExecutorFactory(relay));
        DefaultServerRuntimeContext serverContext = new DefaultServerRuntimeContext(serverEntity, relay, protocolWorker,
                new SimpleComponentRegistry(serverEntity), new DefaultResourceRegistry(), new ServerFeatures(),
                dictionaries, null);

        relay.setServerRuntimeContext(serverContext);

        sessionFactory = new StanzaSessionFactory();
        sessionFactory.setServerRuntimeContext(serverContext);
        sessionFactory.setStanzaProcessor(protocolWorker);
    }

    public void testHandshake() {
        StanzaSession session = sessionFactory.createNewSession();
        session.send(new StanzaBuilder("stream", "http://etherx.jabber.org/streams", "stream")
                .addAttribute("from", "me@vysper.org").addAttribute("to", "vysper.org")
                .declareNamespace("", "jabber:client").build());
        Stanza stanza = waitForStanza(session);
        assertNotNull(stanza);
        System.out.println(DenseStanzaLogRenderer.render(stanza));
        session.send(new StanzaBuilder("starttls", "urn:ietf:params:xml:ns:xmpp-tls")
                .addAttribute("from", "me@vysper.org").build());
        stanza = waitForStanza(session);
        assertNotNull(stanza);
        System.out.println(DenseStanzaLogRenderer.render(stanza));
        session.setIsSecure();
        session.send(new StanzaBuilder("stream", "http://etherx.jabber.org/streams", "stream")
                .addAttribute("from", "me@vysper.org").addAttribute("to", "vysper.org")
                .declareNamespace("", "jabber:client").build());
        stanza = waitForStanza(session);
        assertNotNull(stanza);
        System.out.println(DenseStanzaLogRenderer.render(stanza));
    }

    private Stanza waitForStanza(StanzaSession session) {
        long inTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < inTime + 10000) {
            Stanza stanza = session.poll();
            if (stanza != null)
                return stanza;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
