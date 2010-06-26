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

package org.apache.vysper.xmpp.stanza;

import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * wraps an all-purpose stanza into a core stanza (iq, message, presence)
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
abstract public class XMPPCoreStanza extends Stanza {

    protected static boolean isOfType(Stanza stanza, String name) {
        boolean nameMatches = stanza != null && stanza.getName().equals(name);
        return nameMatches;
    }

    public static XMPPCoreStanza getWrapper(Stanza stanza) {
        if (stanza instanceof XMPPCoreStanza)
            return (XMPPCoreStanza) stanza;
        if (IQStanza.isOfType(stanza))
            return new IQStanza(stanza);
        if (MessageStanza.isOfType(stanza))
            return new MessageStanza(stanza);
        if (PresenceStanza.isOfType(stanza))
            return new PresenceStanza(stanza);
        return null;
    }

    public XMPPCoreStanza(Stanza stanza) {
        super(stanza.getNamespaceURI(), stanza.getName(), stanza.getNamespacePrefix(), stanza.getAttributes(), stanza
                .getInnerFragments());
    }

    @Override
    public XMLElementVerifier getVerifier() {
        if (xmlElementVerifier == null)
            xmlElementVerifier = new XMPPCoreStanzaVerifier(this);
        return xmlElementVerifier;
    }

    public XMPPCoreStanzaVerifier getCoreVerifier() {
        return (XMPPCoreStanzaVerifier) getVerifier();
    }

    @Override
    abstract public String getName();

    public String getType() {
        return getAttributeValue("type");
    }

    public String getID() {
        return getAttributeValue("id");
    }

    public boolean isError() {
        return "error".equals(getType());
    }

    public boolean isServerCall() {
        return getNamespaceURI().equals(NamespaceURIs.JABBER_SERVER);
    }

}
