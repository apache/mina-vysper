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

import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * Logs stanza body texts
 */
public abstract class AbstractBodyTextLogStorageProvider extends AbstractLogStorageProvider {

    public AbstractBodyTextLogStorageProvider() {
        // empty
    }

    public AbstractBodyTextLogStorageProvider(boolean logMessage, boolean logPresence, boolean logIQ) {
        super(logMessage, logPresence, logIQ);
    }

    @Override
    protected void logStanza(Entity from, Entity receiver, XMPPCoreStanza stanza) {
        final List<XMLElement> bodies = stanza.getInnerElementsNamed("body");
        for (final XMLElement body : bodies) {
            final XMLText innerText = body.getInnerText();
            if (innerText != null) {
                logText(from, receiver, innerText.getText());
            }
        }
    }

    protected abstract void logText(Entity from, Entity to, String message);
}
