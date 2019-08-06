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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.in_memory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.EntityFilter;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.SimpleEntityFilter;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class InMemoryEntityFilterTest {

    private static final Entity ROMEO_SIDEWALK = EntityImpl.parseUnchecked("romeo@foo.com/sidewalk");

    private static final Entity ROMEO_CAR = EntityImpl.parseUnchecked("romeo@foo.com/car");

    private static final Entity JULIET_BALCONY = EntityImpl.parseUnchecked("juliet@foo.com/balcony");

    private static final Entity JULIET_TRAIN = EntityImpl.parseUnchecked("juliet@foo.com/train");

    private ArchivedMessage romeoSidewalkToJulietBalcony;

    private ArchivedMessage romeoSidewalkToRomeoCar;

    @Before
    public void before() {
        romeoSidewalkToJulietBalcony = mock(ArchivedMessage.class);
        MessageStanza romeoSidewalkToJulietBalconyStanza = mock(MessageStanza.class);
        when(romeoSidewalkToJulietBalconyStanza.getFrom()).thenReturn(ROMEO_SIDEWALK);
        when(romeoSidewalkToJulietBalconyStanza.getTo()).thenReturn(JULIET_BALCONY);
        when(romeoSidewalkToJulietBalcony.stanza()).thenReturn(romeoSidewalkToJulietBalconyStanza);

        romeoSidewalkToRomeoCar = mock(ArchivedMessage.class);
        MessageStanza romeoSidewalkToRomeoCarStanza = mock(MessageStanza.class);
        when(romeoSidewalkToRomeoCarStanza.getFrom()).thenReturn(ROMEO_SIDEWALK);
        when(romeoSidewalkToRomeoCarStanza.getTo()).thenReturn(ROMEO_CAR);
        when(romeoSidewalkToRomeoCar.stanza()).thenReturn(romeoSidewalkToRomeoCarStanza);
    }

    @Test
    public void toAndFromIgnoringResource() {
        EntityFilter entityFilter = new SimpleEntityFilter(ROMEO_CAR, EntityFilter.Type.TO_AND_FROM, true);
        InMemoryEntityFilter tested = new InMemoryEntityFilter(entityFilter);
        assertTrue(tested.test(romeoSidewalkToRomeoCar));
        assertFalse(tested.test(romeoSidewalkToJulietBalcony));
    }

    @Test
    public void toAndFromNotIgnoringResource() {
        EntityFilter entityFilter = new SimpleEntityFilter(ROMEO_CAR, EntityFilter.Type.TO_AND_FROM, false);
        InMemoryEntityFilter tested = new InMemoryEntityFilter(entityFilter);
        assertFalse(tested.test(romeoSidewalkToRomeoCar));
        assertFalse(tested.test(romeoSidewalkToJulietBalcony));
    }

    @Test
    public void toOrFromIgnoringResource() {
        EntityFilter entityFilter = new SimpleEntityFilter(ROMEO_CAR, EntityFilter.Type.TO_OR_FROM, true);
        InMemoryEntityFilter tested = new InMemoryEntityFilter(entityFilter);
        assertTrue(tested.test(romeoSidewalkToRomeoCar));
        assertTrue(tested.test(romeoSidewalkToJulietBalcony));
    }

    @Test
    public void toOrFromNotIgnoringResource() {
        EntityFilter entityFilter = new SimpleEntityFilter(ROMEO_CAR, EntityFilter.Type.TO_OR_FROM, false);
        InMemoryEntityFilter tested = new InMemoryEntityFilter(entityFilter);
        assertTrue(tested.test(romeoSidewalkToRomeoCar));
        assertFalse(tested.test(romeoSidewalkToJulietBalcony));
    }
}