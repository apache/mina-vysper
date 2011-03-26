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
package org.apache.vysper.xmpp.server.components;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.protocol.AbstractStanzaHandlerLookup;
import org.apache.vysper.xmpp.protocol.DefaultHandlerDictionary;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * look up a component's handler for a stanza 
 */
public class ComponentStanzaHandlerLookup extends AbstractStanzaHandlerLookup {

    private static class ComponentHandlerDictionary extends DefaultHandlerDictionary {

        public ComponentHandlerDictionary() {
            super();
        }
    }

    protected ComponentHandlerDictionary defaultHandlers = new ComponentHandlerDictionary();

    public void addDefaultHandler(StanzaHandler stanzaHandler) {
        defaultHandlers.register(stanzaHandler);
    }

    @Override
    public StanzaHandler getHandler(Stanza stanza) {

        XMLElement firstInnerElement = stanza;
        if (stanza.getVerifier().subElementsPresentExact(1)) {
            firstInnerElement = stanza.getFirstInnerElement();
        }

        StanzaHandler stanzaHandler = getHandlerForElement(stanza, firstInnerElement);

        if (stanzaHandler == null)
            stanzaHandler = defaultHandlers.get(stanza);

        return stanzaHandler;
    }
}
