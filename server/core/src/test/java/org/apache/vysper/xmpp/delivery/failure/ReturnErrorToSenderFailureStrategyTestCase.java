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

import java.util.Arrays;
import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.RecordingStanzaRelay;
import org.apache.vysper.xmpp.delivery.RecordingStanzaRelay.Triple;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 */
public class ReturnErrorToSenderFailureStrategyTestCase extends TestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");
    private static final Entity TO = EntityImpl.parseUnchecked("to@vysper.org");
    private static final Entity SERVER = EntityImpl.parseUnchecked("vysper.org");
    private static final String LANG = "en";
    private static final String BODY = "Hello world";
    private static final String ERROR_TEXT = "Error!";
    
    @SpecCompliant(spec = "draft-ietf-xmpp-3920bis-22", section = "10.4.3", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
    public void testSmartDeliveryException() throws Exception {
        RecordingStanzaRelay relay = new RecordingStanzaRelay();
        ReturnErrorToSenderFailureStrategy strategy = new ReturnErrorToSenderFailureStrategy(relay);
        
        Stanza stanza = XMPPCoreStanza.getWrapper(StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY).build());
        
        strategy.process(stanza, Arrays.asList((DeliveryException)new RemoteServerNotFoundException(ERROR_TEXT)));
        
        Iterator<Triple> triples = relay.iterator();
        Triple triple = triples.next();
        Assert.assertEquals(FROM, triple.getEntity());
        MessageStanza errorStanza = (MessageStanza) XMPPCoreStanza.getWrapper(triple.getStanza());
        Assert.assertEquals("error", errorStanza.getType());
        Assert.assertEquals(FROM, errorStanza.getTo());
        Assert.assertEquals(SERVER, errorStanza.getFrom());
        
        XMLElement errorElm = errorStanza.getSingleInnerElementsNamed("error", NamespaceURIs.JABBER_CLIENT);
        Assert.assertEquals("cancel", errorElm.getAttributeValue("type"));

        Assert.assertNotNull(errorElm.getSingleInnerElementsNamed("remote-server-not-found", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS));

        XMLElement textElm = errorElm.getSingleInnerElementsNamed("text", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        Assert.assertEquals(ERROR_TEXT, textElm.getInnerText().getText());
        
        Assert.assertEquals(IgnoreFailureStrategy.IGNORE_FAILURE_STRATEGY, triple.getDeliveryFailureStrategy());
        
        Assert.assertFalse(triples.hasNext());
    }

    public void testNoException() throws Exception {
        RecordingStanzaRelay relay = new RecordingStanzaRelay();
        ReturnErrorToSenderFailureStrategy strategy = new ReturnErrorToSenderFailureStrategy(relay);
        
        Stanza stanza = XMPPCoreStanza.getWrapper(StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY).build());
        
        strategy.process(stanza, null);
        
        Iterator<Triple> triples = relay.iterator();
        Triple triple = triples.next();
        Assert.assertEquals(FROM, triple.getEntity());
        MessageStanza errorStanza = (MessageStanza) XMPPCoreStanza.getWrapper(triple.getStanza());
        Assert.assertEquals("error", errorStanza.getType());
        Assert.assertEquals(FROM, errorStanza.getTo());
        Assert.assertEquals(SERVER, errorStanza.getFrom());
        
        XMLElement errorElm = errorStanza.getSingleInnerElementsNamed("error", NamespaceURIs.JABBER_CLIENT);
        Assert.assertEquals("cancel", errorElm.getAttributeValue("type"));
        Assert.assertNotNull(errorElm.getSingleInnerElementsNamed("service-unavailable", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS));

        Assert.assertNotNull(errorElm.getSingleInnerElementsNamed("text", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS));
        
        Assert.assertEquals(IgnoreFailureStrategy.IGNORE_FAILURE_STRATEGY, triple.getDeliveryFailureStrategy());
        
        Assert.assertFalse(triples.hasNext());
    }

    
    public void testOnlyIgnoreErrorStanzas() throws Exception {
        RecordingStanzaRelay relay = new RecordingStanzaRelay();
        ReturnErrorToSenderFailureStrategy strategy = new ReturnErrorToSenderFailureStrategy(relay);
        
        Stanza stanza = XMPPCoreStanza.getWrapper(StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY)
            .addAttribute("type", "error")
            .build());

        strategy.process(stanza, Arrays.asList((DeliveryException)new RemoteServerNotFoundException()));

        Assert.assertFalse(relay.iterator().hasNext());
    }
    
    public void testOnlyHandleCoreStanzas() throws Exception {
        RecordingStanzaRelay relay = new RecordingStanzaRelay();
        ReturnErrorToSenderFailureStrategy strategy = new ReturnErrorToSenderFailureStrategy(relay);
        
        Stanza stanza = StanzaBuilder.createMessageStanza(FROM, TO, LANG, BODY).build();
        
        try {
            strategy.process(stanza, Arrays.asList((DeliveryException)new RemoteServerNotFoundException()));
            fail("Must throw DeliveryException");
        } catch(DeliveryException e) {
            // OK
        }
    }

}
