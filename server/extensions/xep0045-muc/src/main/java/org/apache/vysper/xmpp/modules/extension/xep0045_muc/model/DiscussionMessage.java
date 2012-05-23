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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import java.util.Calendar;
import java.util.TimeZone;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Delay;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

public class DiscussionMessage {

    private MessageStanza message;

    private String fromNick;

    private Calendar timestamp;

    public DiscussionMessage(MessageStanza message, Occupant from) {
        this(message, from, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }

    public DiscussionMessage(MessageStanza message, Occupant from, Calendar timestamp) {
        this.message = message;
        this.fromNick = from.getNick();

        this.timestamp = (Calendar) timestamp.clone();
    }

    public Calendar getTimestamp() {
        return (Calendar) timestamp.clone();
    }

    public String getNick() {
        return fromNick;
    }

    public MessageStanza getMessage() {
        return message;
    }

    public Stanza createStanza(Occupant receiver, boolean includeJid) {

        //        <message
        //            from='darkcave@chat.shakespeare.lit/secondwitch'
        //            to='hecate@shakespeare.lit/broom'
        //            type='groupchat'>
        //          <body>Thrice and once the hedge-pig whined.</body>
        //          <delay xmlns='urn:xmpp:delay'
        //             from='wiccarocks@shakespeare.lit/laptop'
        //             stamp='2002-10-13T23:58:43Z'/>
        //        </message>

        Entity roomJid = message.getTo();
        StanzaBuilder builder = StanzaBuilder.createForward(message, new EntityImpl(roomJid, fromNick), receiver
                .getJid());

        Entity delayFrom;
        if (includeJid) {
            delayFrom = message.getFrom();
        } else {
            delayFrom = new EntityImpl(roomJid, fromNick);
        }
        Delay delay = new Delay(delayFrom, timestamp);
        builder.addPreparedElement(delay);

        return builder.build();
    }

    public boolean hasSubject() {
        return !message.getInnerElementsNamed("subject").isEmpty();
    }

    public boolean hasBody() {
        return !message.getInnerElementsNamed("body").isEmpty();
    }

}
