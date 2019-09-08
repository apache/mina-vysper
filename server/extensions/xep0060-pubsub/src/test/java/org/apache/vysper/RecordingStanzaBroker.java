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
package org.apache.vysper;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * @author Réda Housni Alaoui
 */
public class RecordingStanzaBroker implements StanzaBroker {

    private final List<Stanza> stanzasWrittenToSession = new ArrayList<>();

    private StanzaBroker delegate;

    public RecordingStanzaBroker() {

    }

    public RecordingStanzaBroker(StanzaBroker delegate) {
        this.delegate = delegate;
    }

    public Stanza getUniqueStanzaWrittenToSession() {
        assertTrue(stanzasWrittenToSession.size() < 2);
        return stanzasWrittenToSession.stream().findFirst().orElse(null);
    }

    public boolean hasStanzaWrittenToSession() {
        return !stanzasWrittenToSession.isEmpty();
    }

    @Override
    public void write(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {
        if (delegate == null) {
            return;
        }

        delegate.write(receiver, stanza, deliveryFailureStrategy);
    }

    @Override
    public void writeToSession(Stanza stanza) {
        stanzasWrittenToSession.add(stanza);
    }
}
