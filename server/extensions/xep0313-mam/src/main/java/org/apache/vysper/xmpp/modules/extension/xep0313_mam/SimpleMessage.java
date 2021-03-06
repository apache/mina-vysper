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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;

import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.Message;
import org.apache.vysper.xmpp.stanza.MessageStanza;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleMessage implements Message {

    private final MessageStanza stanza;

    private final ZonedDateTime dateTime;

    public SimpleMessage(MessageStanza stanza) {
        this.stanza = requireNonNull(stanza);
        this.dateTime = ZonedDateTime.now();
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
    public String toString() {
        return "SimpleMessage{" + "stanza=" + stanza + ", dateTime=" + dateTime + '}';
    }
}
