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

package org.apache.vysper.xmpp.server.response;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.Anonymous;
import org.apache.vysper.xmpp.authentication.External;
import org.apache.vysper.xmpp.authentication.Plain;
import org.apache.vysper.xmpp.authentication.SASLMechanism;
import org.apache.vysper.xmpp.parser.ParsingException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 */
public class ServerResponsesTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from.org");
    private static final Entity TO = EntityImpl.parseUnchecked("to.org");
    private static final XMPPVersion VERSION = XMPPVersion.VERSION_1_0;
    private SessionContext sessionContext = Mockito.mock(SessionContext.class);

    
    @Test
    public void testFeaturesForAuthentication() throws ParsingException {
        Stanza stanza = new StanzaBuilder("features").startInnerElement("mechanisms",
                NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).startInnerElement("mechanism",
                NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText("EXTERNAL").endInnerElement()
                .startInnerElement("mechanism", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText("PLAIN")
                .endInnerElement().startInnerElement("mechanism", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)
                .addText("ANONYMOUS").endInnerElement().endInnerElement().build();

        ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);
        Mockito.when(sessionContext.getServerRuntimeContext()).thenReturn(serverRuntimeContext);
        
        List<SASLMechanism> mechanismList = new ArrayList<SASLMechanism>();
        mechanismList.add(new External());
        mechanismList.add(new Plain());
        mechanismList.add(new Anonymous());
        // add others
        Assert.assertEquals("stanzas are identical", stanza.toString(), new ServerResponses().getFeaturesForAuthentication(
                mechanismList, sessionContext).toString());
    }
    
    @Test
    public void getStreamOpenerForServerAcceptorInititatedTlsSupported() throws ParsingException {
        Mockito.when(sessionContext.getState()).thenReturn(SessionState.INITIATED);
        
        Stanza expected = new StanzaBuilder("stream", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS, "stream")
            .addAttribute("from", FROM.getFullQualifiedName())
            .addAttribute("version", "1.0")
            .declareNamespace("db", NamespaceURIs.JABBER_SERVER_DIALBACK)
            .declareNamespace("", NamespaceURIs.JABBER_SERVER)
            .startInnerElement("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS)
                .startInnerElement("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).endInnerElement()
                .startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement()
            .endInnerElement()
            .build();

        Stanza actual = new ServerResponses().getStreamOpenerForServerAcceptor(FROM, VERSION, sessionContext, true);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getStreamOpenerForServerAcceptorInititatedTlsNotSupported() throws ParsingException {
        Mockito.when(sessionContext.getState()).thenReturn(SessionState.INITIATED);
        
        Stanza expected = new StanzaBuilder("stream", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS, "stream")
            .addAttribute("from", FROM.getFullQualifiedName())
            .addAttribute("version", "1.0")
            .declareNamespace("db", NamespaceURIs.JABBER_SERVER_DIALBACK)
            .declareNamespace("", NamespaceURIs.JABBER_SERVER)
            .startInnerElement("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS)
                .startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement()
            .endInnerElement()
            .build();

        Stanza actual = new ServerResponses().getStreamOpenerForServerAcceptor(FROM, VERSION, sessionContext, false);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getStreamOpenerForServerAcceptorInititatedNoVersion() throws ParsingException {
        Mockito.when(sessionContext.getState()).thenReturn(SessionState.INITIATED);
        
        Stanza expected = new StanzaBuilder("stream", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS, "stream")
            .addAttribute("from", FROM.getFullQualifiedName())
            .declareNamespace("db", NamespaceURIs.JABBER_SERVER_DIALBACK)
            .declareNamespace("", NamespaceURIs.JABBER_SERVER)
            .build();

        Stanza actual = new ServerResponses().getStreamOpenerForServerAcceptor(FROM, null, sessionContext, true);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getStreamOpenerForServerAcceptorEncryptedTlsSupported() throws ParsingException {
        Mockito.when(sessionContext.getState()).thenReturn(SessionState.ENCRYPTED);
        
        Stanza expected = new StanzaBuilder("stream", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS, "stream")
            .addAttribute("from", FROM.getFullQualifiedName())
            .addAttribute("version", "1.0")
            .declareNamespace("db", NamespaceURIs.JABBER_SERVER_DIALBACK)
            .declareNamespace("", NamespaceURIs.JABBER_SERVER)
            .startInnerElement("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS)
                .startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement()
            .endInnerElement()
            .build();

        Stanza actual = new ServerResponses().getStreamOpenerForServerAcceptor(FROM, VERSION, sessionContext, true);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getStreamOpenerForServerConnector() throws ParsingException {
        Mockito.when(sessionContext.getXMLLang()).thenReturn("sv");
        
        Stanza expected = new StanzaBuilder("stream", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS, "stream")
            .addAttribute("from", FROM.getFullQualifiedName())
            .addAttribute("to", TO.getFullQualifiedName())
            .addAttribute("version", "1.0")
            .addAttribute(NamespaceURIs.XML, "lang", "sv")
            .declareNamespace("db", NamespaceURIs.JABBER_SERVER_DIALBACK)
            .declareNamespace("", NamespaceURIs.JABBER_SERVER)
            .build();

        Stanza actual = new ServerResponses().getStreamOpenerForServerConnector(FROM, TO, VERSION, sessionContext);
        System.out.println(new Renderer(expected).getComplete());
        System.out.println(new Renderer(actual).getComplete());
        Assert.assertEquals(expected, actual);
    }

}
