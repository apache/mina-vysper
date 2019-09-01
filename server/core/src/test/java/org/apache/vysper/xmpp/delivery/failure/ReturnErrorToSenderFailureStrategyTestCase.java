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
package org.apache.vysper.xmpp.delivery.failure;

import static org.apache.vysper.xmpp.stanza.PresenceStanzaType.UNSUBSCRIBED;

import java.util.Arrays;

import org.apache.vysper.StanzaAssert;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 */
public class ReturnErrorToSenderFailureStrategyTestCase extends Mockito {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");
    private static final Entity TO = EntityImpl.parseUnchecked("to@vysper.org");
    private static final Entity SERVER = EntityImpl.parseUnchecked("vysper.org");
    private static final String LANG = "en";
    private static final String BODY = "Hello world";
    private static final String ERROR_TEXT = "Error!";
    
    private StanzaBroker stanzaBroker = mock(StanzaBroker.class);
    private ReturnErrorToSenderFailureStrategy strategy = new ReturnErrorToSenderFailureStrategy(stanzaBroker);

    
    @Test
    @SpecCompliant(spec = "draft-ietf-xmpp-3920bis-22", section = "10.4.3", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
    public void smartDeliveryException() throws Exception {
        Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY).build();
        
        strategy.process(stanza, Arrays.asList((DeliveryException)new RemoteServerNotFoundException(ERROR_TEXT)));
        
        Stanza expected = StanzaBuilder.createMessageStanza(SERVER, FROM, MessageStanzaType.ERROR, null, BODY)
            .startInnerElement("error", NamespaceURIs.JABBER_CLIENT)
            .addAttribute("type", "cancel")
            .startInnerElement("remote-server-not-found", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement()
            .startInnerElement("text", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS)
                .addAttribute(NamespaceURIs.XML, "lang", "en")
                .addText(ERROR_TEXT)
            .endInnerElement()
            .build();

        ArgumentCaptor<Stanza> stanzaCaptor = ArgumentCaptor.forClass(Stanza.class);
        verify(stanzaBroker).write(eq(FROM), stanzaCaptor.capture(), eq(IgnoreFailureStrategy.INSTANCE));
        StanzaAssert.assertEquals(expected, stanzaCaptor.getValue());
    }

    @Test
    public void noException() throws Exception {
        Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY).build();
        
        strategy.process(stanza, null);

        Stanza expected = StanzaBuilder.createMessageStanza(SERVER, FROM, MessageStanzaType.ERROR, null, BODY)
            .startInnerElement("error", NamespaceURIs.JABBER_CLIENT)
            .addAttribute("type", "cancel")
            .startInnerElement("service-unavailable", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement()
            .startInnerElement("text", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS)
            .addAttribute(NamespaceURIs.XML, "lang", "en")
            .addText("stanza could not be delivered")
            .endInnerElement()
            .build();

        ArgumentCaptor<Stanza> stanzaCaptor = ArgumentCaptor.forClass(Stanza.class);
        verify(stanzaBroker).write(eq(FROM), stanzaCaptor.capture(), eq(IgnoreFailureStrategy.INSTANCE));
        StanzaAssert.assertEquals(expected, stanzaCaptor.getValue());
        
    }

    @Test
    public void presenceSubscribe() throws Exception {
        Stanza stanza = StanzaBuilder.createPresenceStanza(FROM, TO, null, PresenceStanzaType.SUBSCRIBE, null, null).build();
        
        DeliveryException e = new NoSuchLocalUserException();
        strategy.process(stanza, Arrays.asList(e));
        
        Stanza expected = StanzaBuilder.createPresenceStanza(TO, FROM, null, UNSUBSCRIBED, null, null).build();
        
        ArgumentCaptor<Stanza> stanzaCaptor = ArgumentCaptor.forClass(Stanza.class);
        verify(stanzaBroker).write(eq(FROM), stanzaCaptor.capture(), eq(IgnoreFailureStrategy.INSTANCE));
        StanzaAssert.assertEquals(expected, stanzaCaptor.getValue());        
    }
    
    @Test
    public void ignorePresenceSubscribed() throws Exception {
        PresenceStanzaType type = PresenceStanzaType.SUBSCRIBED;
        assertPresenceIgnored(type);
    }

    @Test
    public void ignorePresenceUnsubscribe() throws Exception {
        PresenceStanzaType type = PresenceStanzaType.UNSUBSCRIBE;
        assertPresenceIgnored(type);
    }
    
    @Test
    public void ignorePresenceUnsubscribed() throws Exception {
        PresenceStanzaType type = PresenceStanzaType.UNSUBSCRIBED;
        assertPresenceIgnored(type);
    }
    
    @Test
    public void ignorePresenceUnavailable() throws Exception {
        PresenceStanzaType type = PresenceStanzaType.UNAVAILABLE;
        assertPresenceIgnored(type);
    }
    
    @Test
    public void ignorePresenceError() throws Exception {
        PresenceStanzaType type = PresenceStanzaType.ERROR;
        assertPresenceIgnored(type);
    }
    
    private void assertPresenceIgnored(PresenceStanzaType type) throws DeliveryException {
        Stanza stanza = StanzaBuilder.createPresenceStanza(FROM, TO, null, type, null, null).build();
        
        DeliveryException e = new NoSuchLocalUserException();
        strategy.process(stanza, Arrays.asList(e));

        verifyZeroInteractions(stanzaBroker);
    }
    
    @Test
    public void ignoreErrorStanzas() throws Exception {
        Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY)
            .addAttribute("type", "error")
            .build();

        strategy.process(stanza, Arrays.asList((DeliveryException)new RemoteServerNotFoundException()));

        verifyZeroInteractions(stanzaBroker);
    }
    
    @Test
    public void ignoreLocalRecipientOfflineException() throws Exception {
        Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY)
            .build();
        
        strategy.process(stanza, Arrays.asList((DeliveryException)new LocalRecipientOfflineException()));
        
        // TODO Update when fully implemented
        verifyZeroInteractions(stanzaBroker);
    }
    
    @Test(expected=DeliveryException.class)
    public void onlyHandleCoreStanzas() throws Exception {
        Stanza stanza = new StanzaBuilder("dummy", NamespaceURIs.JABBER_CLIENT).build();
        
        strategy.process(stanza, Arrays.asList((DeliveryException)new RemoteServerNotFoundException()));
    }

    @Test(expected=RuntimeException.class)
    public void multipleDeliveryExceptions() throws Exception {
        Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY).build();
        
        DeliveryException e = new RemoteServerNotFoundException();
        
        strategy.process(stanza, Arrays.asList(e, e));
    }

}
