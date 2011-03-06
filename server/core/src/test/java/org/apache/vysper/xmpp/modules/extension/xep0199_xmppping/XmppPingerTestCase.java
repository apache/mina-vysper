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
package org.apache.vysper.xmpp.modules.extension.xep0199_xmppping;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.vysper.StanzaAssert;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;


/**
 */
public class XmppPingerTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");
    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");
    
    private StanzaWriter stanzaWriter = Mockito.mock(StanzaWriter.class);
    private XmppPingIQHandler handler = Mockito.mock(XmppPingIQHandler.class);
    private XmppPingListener listener = Mockito.mock(XmppPingListener.class);
    
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    
    @Test
    public void ping() throws InterruptedException {
        final XmppPinger pinger = new XmppPinger(handler);
        
        executor.execute(new Runnable() {
            public void run() {
                pinger.ping(stanzaWriter, FROM, TO, 10000, listener);
            }
        });
        
        // wait for the ping to be written
        Thread.sleep(1000);
        ArgumentCaptor<Stanza> pingCaptor = ArgumentCaptor.forClass(Stanza.class);
        
        Mockito.verify(stanzaWriter).write(pingCaptor.capture());
        
        Stanza actualPing = pingCaptor.getValue();
        final String pingId = actualPing.getAttributeValue("id");
        Assert.assertNotNull(pingId);
        
        executor.execute(new Runnable() {
            public void run() {
                pinger.pong(pingId);
            }
        });
        
        // wait for the pong to be processed
        Thread.sleep(1000);
        Stanza expectedPing = StanzaBuilder.createIQStanza(FROM, TO, IQStanzaType.GET, pingId)
            .startInnerElement("ping", NamespaceURIs.URN_XMPP_PING)
            .build();
        
        StanzaAssert.assertEquals(expectedPing, actualPing);
        Assert.assertTrue(pingId.startsWith("xmppping-"));

        Mockito.verify(listener).pong();
        Mockito.verifyNoMoreInteractions(listener);
    }

    
    @Test
    public void pingTimeout() {
        final XmppPinger pinger = new XmppPinger(handler);
        pinger.ping(stanzaWriter, FROM, TO, 1000, listener);
        
        ArgumentCaptor<Stanza> pingCaptor = ArgumentCaptor.forClass(Stanza.class);
        
        Mockito.verify(stanzaWriter).write(pingCaptor.capture());
        
        Stanza actualPing = pingCaptor.getValue();
        final String pingId = actualPing.getAttributeValue("id");
        
        Assert.assertNotNull(pingId);
        
        executor.execute(new Runnable() {
            public void run() {
                pinger.pong(pingId);
            }
        });
        
        Stanza expectedPing = StanzaBuilder.createIQStanza(FROM, TO, IQStanzaType.GET, pingId)
            .startInnerElement("ping", NamespaceURIs.URN_XMPP_PING)
            .build();
        
        StanzaAssert.assertEquals(expectedPing, actualPing);
        Assert.assertTrue(pingId.startsWith("xmppping-"));

        Mockito.verify(listener).timeout();
        Mockito.verifyNoMoreInteractions(listener);
    }

}
