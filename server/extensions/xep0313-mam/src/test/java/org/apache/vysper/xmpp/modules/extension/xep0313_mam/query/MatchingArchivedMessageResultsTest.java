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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.StanzaAssert;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessages;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.SimpleArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.SimpleArchivedMessages;
import org.apache.vysper.xmpp.parser.XMLParserUtil;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author Réda Housni Alaoui
 */
public class MatchingArchivedMessageResultsTest {

    private static final Entity INITIATING_ENTITY = EntityImpl.parseUnchecked("juliet@capulet.lit/chamber");

    private static final Entity ARCHIVE_ID = EntityImpl.parseUnchecked("juliet@capulet.lit");

    private Query query;

    private MessageStanza messageStanza;

    @Before
    public void before() throws XMLSemanticError, IOException, SAXException {
        XMLElement queryIqElement = XMLParserUtil.parseRequiredDocument(
                "<iq type='set' id='juliet1'><query xmlns='urn:xmpp:mam:2' queryid='f27'/></iq>");
        query = new Query("urn:xmpp:mam:2",
                new IQStanza(StanzaBuilder.createClone(queryIqElement, true, Collections.emptyList()).build()));

        XMLElement messageElement = XMLParserUtil.parseRequiredDocument(
                "<message xmlns='jabber:client' from=\"witch@shakespeare.lit\" to=\"macbeth@shakespeare.lit\">"
                        + "<body>Hail to thee</body></message>");
        messageStanza = new MessageStanza(
                StanzaBuilder.createClone(messageElement, true, Collections.emptyList()).build());
    }

    @Test
    public void testUncomplete() throws IOException, SAXException {
        SimpleArchivedMessage archivedMessage1 = new SimpleArchivedMessage("28482-98726-73623",
                ZonedDateTime.of(LocalDateTime.of(2010, 7, 10, 23, 8, 25), ZoneId.of("Z")), messageStanza);
        SimpleArchivedMessage archivedMessage2 = new SimpleArchivedMessage("09af3-cc343-b409f",
                ZonedDateTime.of(LocalDateTime.of(2010, 7, 10, 23, 8, 25), ZoneId.of("Z")), messageStanza);

        List<ArchivedMessage> archivedMessagesList = new ArrayList<>();
        archivedMessagesList.add(archivedMessage1);
        archivedMessagesList.add(archivedMessage2);

        SimpleArchivedMessages archivedMessages = new SimpleArchivedMessages(archivedMessagesList, 0L, 3L);

        MatchingArchivedMessageResults tested = new MatchingArchivedMessageResults(INITIATING_ENTITY, ARCHIVE_ID, query,
                archivedMessages);

        List<Stanza> responseStanzas = tested.toStanzas();
        assertEquals(3, responseStanzas.size());

        StanzaAssert.assertEquals(StanzaBuilder
                .createClone(XMLParserUtil.parseRequiredDocument("<message to='juliet@capulet.lit/chamber'>"
                        + "  <result xmlns='urn:xmpp:mam:2' queryid='f27' id='28482-98726-73623'>"
                        + "    <forwarded xmlns='urn:xmpp:forward:0'>"
                        + "      <delay xmlns='urn:xmpp:delay' stamp='2010-07-10T23:08:25Z'/>"
                        + "      <message xmlns='jabber:client' from='witch@shakespeare.lit' to='macbeth@shakespeare.lit'>"
                        + "        <body>Hail to thee</body>"
                        + "        <stanza-id xmlns='urn:xmpp:sid:0' by='juliet@capulet.lit' id='28482-98726-73623'/>"
                        + "</message></forwarded></result></message>"), true, null)
                .build(), responseStanzas.get(0));

        StanzaAssert.assertEquals(StanzaBuilder
                .createClone(XMLParserUtil.parseRequiredDocument("<message to='juliet@capulet.lit/chamber'>"
                        + "  <result xmlns='urn:xmpp:mam:2' queryid='f27' id='09af3-cc343-b409f'>"
                        + "    <forwarded xmlns='urn:xmpp:forward:0'>"
                        + "      <delay xmlns='urn:xmpp:delay' stamp='2010-07-10T23:08:25Z'/>"
                        + "      <message xmlns='jabber:client' from='witch@shakespeare.lit' to='macbeth@shakespeare.lit'>"
                        + "        <body>Hail to thee</body>"
                        + "        <stanza-id xmlns='urn:xmpp:sid:0' by='juliet@capulet.lit' id='09af3-cc343-b409f'/>"
                        + "</message></forwarded></result></message>"), true, null)
                .build(), responseStanzas.get(1));

        StanzaAssert.assertEquals(
                StanzaBuilder.createClone(XMLParserUtil
                        .parseRequiredDocument("<iq type='result' id='juliet1'><fin xmlns='urn:xmpp:mam:2'>"
                                + "    <set xmlns='http://jabber.org/protocol/rsm'>"
                                + "      <count>3</count><first index='0'>28482-98726-73623</first>"
                                + "      <last>09af3-cc343-b409f</last></set></fin></iq>"),
                        true, null).build(),
                responseStanzas.get(2));
    }

    @Test
    public void testComplete() throws IOException, SAXException {
        ArchivedMessage message = new SimpleArchivedMessage("28482-98726-73623",
                ZonedDateTime.of(LocalDateTime.of(2010, 7, 10, 23, 8, 25), ZoneId.of("Z")), messageStanza);

        ArchivedMessages archivedMessages = new SimpleArchivedMessages(Collections.singletonList(message), 0L, 1L);

        MatchingArchivedMessageResults tested = new MatchingArchivedMessageResults(INITIATING_ENTITY, ARCHIVE_ID, query,
                archivedMessages);

        List<Stanza> responseStanzas = tested.toStanzas();
        assertEquals(2, responseStanzas.size());

        StanzaAssert.assertEquals(StanzaBuilder
                .createClone(XMLParserUtil.parseRequiredDocument("<message to='juliet@capulet.lit/chamber'>"
                        + "  <result xmlns='urn:xmpp:mam:2' queryid='f27' id='28482-98726-73623'>"
                        + "    <forwarded xmlns='urn:xmpp:forward:0'>"
                        + "      <delay xmlns='urn:xmpp:delay' stamp='2010-07-10T23:08:25Z'/>"
                        + "      <message xmlns='jabber:client' from='witch@shakespeare.lit' to='macbeth@shakespeare.lit'>"
                        + "        <body>Hail to thee</body>"
                        + "        <stanza-id xmlns='urn:xmpp:sid:0' by='juliet@capulet.lit' id='28482-98726-73623'/>"
                        + "</message></forwarded></result></message>"), true, null)
                .build(), responseStanzas.get(0));

        StanzaAssert.assertEquals(
                StanzaBuilder.createClone(XMLParserUtil.parseRequiredDocument(
                        "<iq type='result' id='juliet1'><fin xmlns='urn:xmpp:mam:2' complete='true'>"
                                + "    <set xmlns='http://jabber.org/protocol/rsm'>"
                                + "      <count>1</count><first index='0'>28482-98726-73623</first>"
                                + "      <last>28482-98726-73623</last></set></fin></iq>"),
                        true, null).build(),
                responseStanzas.get(1));
    }

    @Test
    public void testEmptyPage() throws IOException, SAXException {
        SimpleArchivedMessages archivedMessages = new SimpleArchivedMessages(Collections.emptyList(), 0L, 50L);

        MatchingArchivedMessageResults tested = new MatchingArchivedMessageResults(INITIATING_ENTITY, ARCHIVE_ID, query,
                archivedMessages);

        List<Stanza> responseStanzas = tested.toStanzas();
        assertEquals(1, responseStanzas.size());

        StanzaAssert.assertEquals(
                StanzaBuilder.createClone(XMLParserUtil
                        .parseRequiredDocument("<iq type='result' id='juliet1'><fin xmlns='urn:xmpp:mam:2'>"
                                + "    <set xmlns='http://jabber.org/protocol/rsm'>"
                                + "      <count>50</count></set></fin></iq>"),
                        true, null).build(),
                responseStanzas.get(0));
    }

}