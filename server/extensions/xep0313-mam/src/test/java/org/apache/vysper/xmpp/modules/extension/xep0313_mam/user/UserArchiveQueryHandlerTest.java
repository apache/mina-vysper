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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.ServerRuntimeContextMock;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.SessionContextMock;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.query.Query;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchivesMock;
import org.apache.vysper.xmpp.parser.XMLParserUtil;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author Réda Housni Alaoui
 */
public class UserArchiveQueryHandlerTest {

    private static final Entity JULIET = EntityImpl.parseUnchecked("juliet@foo.com/balcony");

    private static final Entity ROMEO = EntityImpl.parseUnchecked("romeo@foo.com/floor");

    private static final Entity INITIATING_ENTITY = JULIET;

    private Query romeoTargetingQuery;

    private Query untargetedQuery;

    private MessageArchivesMock archives;

    private ServerRuntimeContextMock serverRuntimeContext;

    private SessionContextMock sessionContext;

    private UserArchiveQueryHandler tested;

    @Before
    public void before() throws IOException, SAXException, XMLSemanticError {
        romeoTargetingQuery = new Query("urn:xmpp:mam:2",
                new IQStanza(StanzaBuilder.createClone(
                        XMLParserUtil.parseRequiredDocument(
                                "<iq type='set' to='romeo@foo.com'><query xmlns='urn:xmpp:mam:2'/></iq>"),
                        true, Collections.emptyList()).build()));

        untargetedQuery = new Query("urn:xmpp:mam:2",
                new IQStanza(StanzaBuilder.createClone(
                        XMLParserUtil.parseRequiredDocument("<iq type='set'><query xmlns='urn:xmpp:mam:2'/></iq>"),
                        true, Collections.emptyList()).build()));

        serverRuntimeContext = new ServerRuntimeContextMock(EntityImpl.parseUnchecked("capulet.lit"));
        archives = serverRuntimeContext.givenUserMessageArchives();
        sessionContext = new SessionContextMock();
        sessionContext.givenInitiatingEntity(INITIATING_ENTITY);
        tested = new UserArchiveQueryHandler();
    }

    @Test
    public void supportsAllQueries() {
        assertTrue(tested.supports(null, null, null));
    }

    @Test
    public void untargetedQueryEndsUpInInitiatingEntityArchive() throws XMLSemanticError {
        archives.givenArchive(INITIATING_ENTITY.getBareJID());
        List<Stanza> stanzas = tested.handle(untargetedQuery, serverRuntimeContext, sessionContext);
        assertEquals(1, stanzas.size());
        Stanza stanza = stanzas.get(0);
        assertEquals("iq", stanza.getName());
        assertNotNull(stanza.getSingleInnerElementsNamed("fin"));
    }

    @Test
    public void preventsUserFromQueryingOtherUserArchive() throws XMLSemanticError {
        List<Stanza> stanzas = tested.handle(romeoTargetingQuery, serverRuntimeContext, sessionContext);
        assertError(stanzas, StanzaErrorCondition.FORBIDDEN);
    }

    @Test
    public void unexistingArchiveLeadsToItemNotFound() throws XMLSemanticError {
        List<Stanza> stanzas = tested.handle(untargetedQuery, serverRuntimeContext, sessionContext);
        assertError(stanzas, StanzaErrorCondition.ITEM_NOT_FOUND);
    }

    private void assertError(List<Stanza> stanzas, StanzaErrorCondition errorCondition) throws XMLSemanticError {
        assertEquals(1, stanzas.size());
        XMLElement error = stanzas.get(0).getSingleInnerElementsNamed("error");
        assertNotNull(error);
        assertNotNull(error.getSingleInnerElementsNamed(errorCondition.value()));
    }

}