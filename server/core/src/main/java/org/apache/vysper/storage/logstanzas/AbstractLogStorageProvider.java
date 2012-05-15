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
package org.apache.vysper.storage.logstanzas;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * logs stanzas
 */
public abstract class AbstractLogStorageProvider implements LogStorageProvider {
    protected boolean logMessage = true;
    protected boolean logPresence = false;
    protected boolean logIQ = false;

    public AbstractLogStorageProvider() {
        // empty
    }

    public AbstractLogStorageProvider(boolean logMessage, boolean logPresence, boolean logIQ) {
        this.logMessage = logMessage;
        this.logPresence = logPresence;
        this.logIQ = logIQ;
    }
    
    public void logStanza(final Entity receiver, final Stanza stanza) {
        final XMPPCoreStanza coreStanza = XMPPCoreStanza.getWrapper(stanza);
        if (coreStanza == null) return;
        
        // skip stanza types which are not logged
        if (!logMessage && coreStanza instanceof MessageStanza) return;
        if (!logPresence && coreStanza instanceof PresenceStanza) return;
        if (!logIQ && coreStanza instanceof IQStanza) return;
        
        final Entity from = stanza.getFrom();
        logStanza(from, receiver, coreStanza);
    }

    protected abstract void logStanza(Entity from, Entity receiver, XMPPCoreStanza stanza);
}
