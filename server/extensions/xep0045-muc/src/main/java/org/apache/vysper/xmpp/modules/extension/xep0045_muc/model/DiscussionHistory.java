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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.History;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * The discussion history for a room
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DiscussionHistory implements Iterable<DiscussionMessage> {

    public static final int DEFAULT_HISTORY_SIZE = 20;

    private int maxItems = DEFAULT_HISTORY_SIZE;

    private DiscussionMessage subjectMessage;

    private List<DiscussionMessage> items = new ArrayList<DiscussionMessage>();

    public void append(MessageStanza message, Occupant sender) {
        append(message, sender, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }

    public void append(MessageStanza message, Occupant sender, Calendar timestamp) {
        synchronized (items) {
            DiscussionMessage discMsg = new DiscussionMessage(message, sender, timestamp);

            if (discMsg.hasSubject() && !discMsg.hasBody()) {
                subjectMessage = discMsg;
            } else {
                items.add(discMsg);
            }

            // check if size is over limits
            while (maxItems != -1 && getSize() > maxItems) {
                items.remove(0) ; // oldest
            }
        }
    }

    public void setMaxItems(int maxItems) {
        if (maxItems < -1) maxItems = -1;
        this.maxItems = maxItems;
    }

    private int getSize() {
        int size = items.size();
        if (subjectMessage != null) size++;
        return size;
    }

    public List<Stanza> createStanzas(Occupant receiver, boolean includeJid, History history) {
        synchronized (items) {

            int maxstanzas = history != null && history.getMaxStanzas() != null ? history.getMaxStanzas() : -1;
            int maxchars = history != null && history.getMaxChars() != null ? history.getMaxChars() : -1;
            int seconds = history != null && history.getSeconds() != null ? history.getSeconds() : -1;

            List<Stanza> stanzas = new ArrayList<Stanza>();

            if (maxchars == 0 || maxstanzas == 0 || seconds == 0) {
                // quick return for no-stanza requests
                return Collections.emptyList();
            } else {
                int counter = 0;
                int totalChars = 0;

                List<DiscussionMessage> itemsWithSubject = new ArrayList<DiscussionMessage>(items);
                // add subject if one is provided
                if (subjectMessage != null) {
                    itemsWithSubject.add(subjectMessage);
                }

                // the timestamp at which "seconds" start filtering from 
                long secondsLimit = -1;
                if (seconds != -1) {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal.add(Calendar.SECOND, -seconds);
                    secondsLimit = cal.getTimeInMillis();
                }

                // now add all messages, as long as the predicated are fulfilled
                // first, do this in reverse order so that older messages are filtered out
                for (int i = itemsWithSubject.size() - 1; i > -1; i--) {
                    DiscussionMessage item = itemsWithSubject.get(i);
                    Stanza stanza = item.createStanza(receiver, includeJid);
                    counter++;

                    if (secondsLimit != -1 && secondsLimit > item.getTimestamp().getTimeInMillis()) {
                        // too old, break
                        break;
                    }

                    if (history != null && history.getSince() != null && history.getSince().after(item.getTimestamp())) {
                        // too old, break
                        break;
                    }

                    // only count chars if needed
                    if (maxchars != -1) {
                        totalChars += new Renderer(stanza).getComplete().length();

                        if (totalChars > maxchars) {
                            break;
                        }
                    }

                    // checks after this line will include the last stanza
                    stanzas.add(stanza);
                    if (maxstanzas != -1 && counter == maxstanzas) {
                        // max number of stanzas reached, return
                        break;
                    }
                }
            }
            // reverse list so that the oldest message is first
            Collections.reverse(stanzas);
            return stanzas;
        }
    }

    public Iterator<DiscussionMessage> iterator() {
        return items.iterator();
    }
}
