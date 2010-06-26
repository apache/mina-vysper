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
package org.apache.vysper.xmpp.modules.roster;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * factory methods for roster related stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class RosterStanzaUtils {

    /**
     * creates IQ stanza containing the given roster items.
     * the server returns this stanza when the client requests or changes the roster
     */
    public static StanzaBuilder createRosterItemsIQ(Entity to, String id, IQStanzaType type,
            Iterable<RosterItem> rosterItems) {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(null, to, type, id).startInnerElement("query",
                NamespaceURIs.JABBER_IQ_ROSTER);

        for (RosterItem rosterItem : rosterItems) {
            createRosterItem(stanzaBuilder, rosterItem);
        }

        stanzaBuilder.endInnerElement();

        return stanzaBuilder;
    }

    /**
     * create IQ stanza for pushing a roster item update to the client
     */
    public static Stanza createRosterItemPushIQ(Entity to, String id, RosterItem rosterItem) {
        return createRosterItemIQ(to, id, IQStanzaType.SET, rosterItem);
    }

    /**
     * general purpose factory method to create a roster IQ stanza for sending roster item information to the client
     */
    public static Stanza createRosterItemIQ(Entity to, String id, IQStanzaType iqStanzaType, RosterItem rosterItem) {
        List<RosterItem> itemList = new ArrayList<RosterItem>();
        itemList.add(rosterItem);
        return createRosterItemsIQ(to, id, iqStanzaType, itemList).build();
    }

    /**
     * adds the given roster item to the roster stanza in creation
     * @param stanzaBuilder represents the stanza in creation
     * @param rosterItem the added roster item
     */
    public static void createRosterItem(StanzaBuilder stanzaBuilder, RosterItem rosterItem) {
        stanzaBuilder.startInnerElement("item", NamespaceURIs.JABBER_IQ_ROSTER).addAttribute("jid",
                rosterItem.getJid().getFullQualifiedName());
        if (rosterItem.getName() != null) {
            stanzaBuilder.addAttribute("name", rosterItem.getName());
        }
        if (rosterItem.getSubscriptionType() != null) {
            stanzaBuilder.addAttribute("subscription", rosterItem.getSubscriptionType().value());
        }
        if (rosterItem.getAskSubscriptionType() != AskSubscriptionType.NOT_SET) {
            stanzaBuilder.addAttribute("ask", rosterItem.getAskSubscriptionType().value());
        }
        List<RosterGroup> groupList = rosterItem.getGroups();
        if (groupList != null) {
            for (RosterGroup rosterGroup : groupList) {
                stanzaBuilder.startInnerElement("group", NamespaceURIs.JABBER_IQ_ROSTER).addText(rosterGroup.getName())
                        .endInnerElement();
            }
        }
        stanzaBuilder.endInnerElement();
    }

}
