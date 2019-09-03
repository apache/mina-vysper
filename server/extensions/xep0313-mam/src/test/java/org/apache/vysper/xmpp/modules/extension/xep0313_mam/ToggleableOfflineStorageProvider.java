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

import java.util.Collection;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.MemoryOfflineStorageProvider;
import org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.OfflineStorageProvider;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * @author Réda Housni Alaoui
 */
public class ToggleableOfflineStorageProvider implements OfflineStorageProvider {

    private final MemoryOfflineStorageProvider memoryOfflineStorageProvider;

    private boolean disabled;

    public void disable() {
        disabled = true;
    }

    public ToggleableOfflineStorageProvider() {
        this.memoryOfflineStorageProvider = new MemoryOfflineStorageProvider();
    }

    @Override
    public Collection<Stanza> getStanzasFor(Entity jid) {
        return memoryOfflineStorageProvider.getStanzasFor(jid);
    }

    @Override
    public void receive(Stanza stanza) {
        if (disabled) {
            return;
        }
        memoryOfflineStorageProvider.receive(stanza);
    }

}
