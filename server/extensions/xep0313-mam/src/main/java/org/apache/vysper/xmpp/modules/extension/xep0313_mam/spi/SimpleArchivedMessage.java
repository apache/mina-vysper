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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;

import org.apache.vysper.xmpp.stanza.MessageStanza;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleArchivedMessage implements ArchivedMessage {

    private final String id;

    private final ZonedDateTime dateTime;

    private final MessageStanza stanza;

    public SimpleArchivedMessage(String id, Message message) {
        this(id, message.dateTime(), message.stanza());
    }

    public SimpleArchivedMessage(String id, ZonedDateTime dateTime, MessageStanza stanza) {
        this.id = requireNonNull(id);
        this.dateTime = requireNonNull(dateTime);
        this.stanza = requireNonNull(stanza);
    }

    @Override
    public MessageStanza stanza() {
        return stanza;
    }

    @Override
    public ZonedDateTime dateTime() {
        return dateTime;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleArchivedMessage that = (SimpleArchivedMessage) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
