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
package org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage;

import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOfflineStorageProvider implements OfflineStorageProvider {

    final Logger logger = LoggerFactory.getLogger(AbstractOfflineStorageProvider.class);

    /** checks if a stanza should be stored for offline receivers */
    public void receive(Stanza stanza) {
        stanza = XMPPCoreStanza.getWrapper(stanza);
        
        // according to XEP-0160 only certain stanzas should be stored
        boolean store = false;
        logger.debug("Received Stanza for offline storage:" + stanza.getClass().getSimpleName());
        if (stanza instanceof MessageStanza) {
            MessageStanza messageStanza = (MessageStanza) stanza;
            MessageStanzaType type = messageStanza.getMessageType();
            
            switch (type) {
            case NORMAL:
            case CHAT:
                store = true;
                break;
            case GROUPCHAT:
            case ERROR:
            case HEADLINE:
                store = false;
                break;
            default:
                throw new RuntimeException("unknown mesage type " + type);
            }
        } else if (stanza instanceof PresenceStanza) {
            PresenceStanza presenceStanza = (PresenceStanza) stanza;
            PresenceStanzaType type = presenceStanza.getPresenceType();
            switch (type) {
            case SUBSCRIBE:
            case SUBSCRIBED:
            case UNSUBSCRIBE:
            case UNSUBSCRIBED:
                store = true;
                break;
            case ERROR:
            case PROBE:
            case UNAVAILABLE:
                store = false;
                break;
            default:
                throw new RuntimeException("unknown presence type " + type);
            }
        }
        if (!store) {
            logger.debug("Stanza is not intended for offline storage");
            return;
        }
        logger.debug("Stanza will be stored offline");
        storeStanza(stanza);
    }

    /** does the actual storage mechanism */
    protected abstract void storeStanza(Stanza stanza);

}
