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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public abstract class AbstractStanzaGenerator {

    /**
     * Override and provide the Namespace the pubsub element lies within.
     * 
     * @return the namespace for the IQ stanza as String
     */
    protected abstract String getNamespace();

    /**
     * Override and provide a optional inner element (within the IQ/pubsub elements).
     * 
     * @param client the requesting client
     * @param pubsubService the JID of the pubsub service
     * @param sb the StanzaBuilder currently used
     * @param node the name of the pubsub node
     * @return the (modified) StanzaBuilder
     */
    protected abstract StanzaBuilder buildInnerElement(Entity client, Entity pubsubService, StanzaBuilder sb,
            String node);

    /**
     * Override and define the IQ stanza's type (get or set)
     * 
     * @return Type of Stanza @see {@link IQStanzaType}
     */
    protected abstract IQStanzaType getStanzaType();

    /**
     * Creates a Stanza wrapper for the publish/subscribe extension.
     * 
     * @param client JID of the client
     * @param pubsub JID of the pubsub Service
     * @param id ID for the Stanza
     * @param node the name of the node
     * @return the generated stanza
     */
    public Stanza getStanza(Entity client, Entity pubsub, String id, String node) {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(client, pubsub, getStanzaType(), id);
        stanzaBuilder.startInnerElement("pubsub", getNamespace());

        buildInnerElement(client, pubsub, stanzaBuilder, node);

        stanzaBuilder.endInnerElement();

        return stanzaBuilder.build();
    }
}
