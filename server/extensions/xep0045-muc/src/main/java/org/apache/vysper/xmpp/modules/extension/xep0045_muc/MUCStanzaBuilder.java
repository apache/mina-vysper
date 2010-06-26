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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import java.util.List;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * Specialized {@link StanzaBuilder} for MUC
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCStanzaBuilder extends StanzaBuilder {

    public static Stanza createPresenceStanza(Entity from, Entity to, PresenceStanzaType type, String xNamespaceUri,
            List<XMLElement> innerElms) {
        return createPresenceStanza(from, to, type, xNamespaceUri, innerElms.toArray(new XMLElement[0]));
    }

    public static Stanza createPresenceStanza(Entity from, Entity to, PresenceStanzaType type, String xNamespaceUri,
            XMLElement... innerElms) {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(from, to, null, type, null, null);
        builder.addPreparedElement(new X(xNamespaceUri, innerElms));

        return builder.build();
    }

    public MUCStanzaBuilder(String stanzaName, String namespaceURI, List<Attribute> attributes,
            List<XMLFragment> innerFragments) {
        super(stanzaName, namespaceURI, null, attributes, innerFragments);
    }

    public MUCStanzaBuilder(String stanzaName, String namespaceURI) {
        super(stanzaName, namespaceURI);
    }
}
