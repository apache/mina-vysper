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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.muc.MUCArchiveQueryHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.user.UserArchiveQueryHandler;
import org.apache.vysper.xmpp.parser.XMLParserUtil;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author Réda Housni Alaoui
 */
public class MAMIQHandlerTest {

    private QueryHandler pubsubNodeArchiveQueryHandler;

    private QueryHandler mucArchiveQueryHandler;

    private QueryHandler userArchiveQueryHandler;

    private IQStanza stanza;

    private ServerRuntimeContext serverRuntimeContext;

    private SessionContext sessionContext;

    private MAMIQQueryHandler tested;

    @Before
    public void before() throws IOException, SAXException {
        pubsubNodeArchiveQueryHandler = mock(QueryHandler.class);
        mucArchiveQueryHandler = mock(MUCArchiveQueryHandler.class);
        userArchiveQueryHandler = mock(UserArchiveQueryHandler.class);

        tested = new MAMIQQueryHandler("urn:xmpp:mam:2", pubsubNodeArchiveQueryHandler, mucArchiveQueryHandler,
                userArchiveQueryHandler);

        XMLElement queryIqElement = XMLParserUtil.parseRequiredDocument(
                "<iq type='set' id='juliet1'><query xmlns='urn:xmpp:mam:2' queryid='f27'/></iq>");
        stanza = new IQStanza(StanzaBuilder.createClone(queryIqElement, true, Collections.emptyList()).build());
        serverRuntimeContext = mock(ServerRuntimeContext.class);
        sessionContext = mock(SessionContext.class);
    }

    @Test
    public void verifySupportStanza() {
        assertTrue(tested.verifyInnerElement(stanza));
    }

    @Test
    public void verifyUnsupportStanzaNamespace() throws IOException, SAXException {
        XMLElement queryIqElement = XMLParserUtil
                .parseRequiredDocument("<iq type='set' id='juliet1'><query queryid='f27'/></iq>");
        Stanza stanza = StanzaBuilder.createClone(queryIqElement, true, Collections.emptyList()).build();

        assertFalse(tested.verifyInnerElement(stanza));
    }

    @Test
    public void verifyUnsupportStanzaName() throws IOException, SAXException {
        XMLElement queryIqElement = XMLParserUtil
                .parseRequiredDocument("<iq type='set' id='juliet1'><foo xmlns='urn:xmpp:mam:2' queryid='f27'/></iq>");
        Stanza stanza = StanzaBuilder.createClone(queryIqElement, true, Collections.emptyList()).build();

        assertFalse(tested.verifyInnerElement(stanza));
    }

    @Test
    public void pubsubHasHighestPriority() {
        when(pubsubNodeArchiveQueryHandler.supports(any(), any(), any())).thenReturn(true);
        when(mucArchiveQueryHandler.supports(any(), any(), any())).thenReturn(true);
        when(userArchiveQueryHandler.supports(any(), any(), any())).thenReturn(true);

        tested.handleSet(stanza, serverRuntimeContext, sessionContext, null);
        verify(pubsubNodeArchiveQueryHandler).handle(any(), any(), any());
        verify(mucArchiveQueryHandler, never()).handle(any(), any(), any());
        verify(userArchiveQueryHandler, never()).handle(any(), any(), any());
    }

    @Test
    public void mucHasLessPriorityThanPubsub() {
        when(pubsubNodeArchiveQueryHandler.supports(any(), any(), any())).thenReturn(false);
        when(mucArchiveQueryHandler.supports(any(), any(), any())).thenReturn(true);
        when(userArchiveQueryHandler.supports(any(), any(), any())).thenReturn(true);

        tested.handleSet(stanza, serverRuntimeContext, sessionContext, null);
        verify(pubsubNodeArchiveQueryHandler, never()).handle(any(), any(), any());
        verify(mucArchiveQueryHandler).handle(any(), any(), any());
        verify(userArchiveQueryHandler, never()).handle(any(), any(), any());
    }

    @Test
    public void userHasLowestPriority() {
        when(pubsubNodeArchiveQueryHandler.supports(any(), any(), any())).thenReturn(false);
        when(mucArchiveQueryHandler.supports(any(), any(), any())).thenReturn(false);
        when(userArchiveQueryHandler.supports(any(), any(), any())).thenReturn(true);

        tested.handleSet(stanza, serverRuntimeContext, sessionContext, null);
        verify(pubsubNodeArchiveQueryHandler, never()).handle(any(), any(), any());
        verify(mucArchiveQueryHandler, never()).handle(any(), any(), any());
        verify(userArchiveQueryHandler).handle(any(), any(), any());
    }

}