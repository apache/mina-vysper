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

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.EntityFilter;
import org.apache.vysper.xmpp.stanza.MessageStanza;

/**
 * @author Réda Housni Alaoui
 */
public class InMemoryEntityFilter implements Predicate<ArchivedMessage> {

    private final EntityFilter filter;

    public InMemoryEntityFilter(EntityFilter filter) {
        this.filter = requireNonNull(filter);
    }

    @Override
    public boolean test(ArchivedMessage message) {
        EntityFilter.Type type = filter.type();
        if (type == EntityFilter.Type.TO_AND_FROM) {
            return matchToAndFrom(message);
        } else if (type == EntityFilter.Type.TO_OR_FROM) {
            return matchToOrFrom(message);
        } else {
            throw new IllegalArgumentException("Unexpected entity filter type '" + type + "'");
        }
    }

    private boolean matchToAndFrom(ArchivedMessage message) {
        MessageStanza stanza = message.stanza();
        return entitiesEquals(filter.entity(), stanza.getTo()) && entitiesEquals(filter.entity(), stanza.getFrom());
    }

    private boolean matchToOrFrom(ArchivedMessage message) {
        MessageStanza stanza = message.stanza();
        return entitiesEquals(filter.entity(), stanza.getTo()) || entitiesEquals(filter.entity(), stanza.getFrom());
    }

    private boolean entitiesEquals(Entity entity1, Entity entity2) {
        if (filter.ignoreResource()) {
            return entity1.getBareJID().equals(entity2.getBareJID());
        } else {
            return entity1.equals(entity2);
        }
    }
}
