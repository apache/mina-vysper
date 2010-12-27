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
package org.apache.vysper.xmpp.protocol;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * Default implementation of {@link HandlerDictionary}. Will simple check all handlers
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultHandlerDictionary implements HandlerDictionary {

    private List<StanzaHandler> handlerList = new ArrayList<StanzaHandler>();

    private boolean sealed = false;

    public DefaultHandlerDictionary() {
    }

    public DefaultHandlerDictionary(List<StanzaHandler> handlerList) {
        if (handlerList != null) {
            for (StanzaHandler stanzaHandler : handlerList) {
                register(stanzaHandler);
            }
        }
        seal();
    }

    public DefaultHandlerDictionary(StanzaHandler stanzaHandler) {
        register(stanzaHandler);
        seal();
    }

    public void register(StanzaHandler stanzaHandler) {
        if (sealed)
            throw new IllegalStateException("stanza directory is sealed. registering denied.");
        if (stanzaHandler == null || stanzaHandler.getName() == null)
            throw new IllegalArgumentException("stanza handler not complete");

        if (handlerList.contains(stanzaHandler))
            throw new IllegalStateException("stanza handler already in handlerList: " + stanzaHandler.getName());
        handlerList.add(stanzaHandler);
    }

    public void seal() {
        sealed = true;
    }

    /**
     * returns the first handler whose verify method returns true for the given stanza
     * @param stanza
     */
    public StanzaHandler get(Stanza stanza) {
        for (StanzaHandler stanzaHandler : handlerList) {
            if (stanzaHandler.verify(stanza))
                return stanzaHandler;
        }
        return null;
    }
}
