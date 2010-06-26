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

import java.util.Map;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;

/**
 * presence stanza (publish/subscribe [aka "pub/sub"] or broadcast)
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PresenceStanza extends XMPPCoreStanza {
    public static final String NAME = "presence";

    public static boolean isOfType(Stanza stanza) {
        return isOfType(stanza, NAME);
    }

    public PresenceStanza(Stanza stanza) {
        super(stanza);
        if (!PresenceStanza.isOfType(stanza))
            throw new IllegalArgumentException("only 'presence' stanza is allowed here");
    }

    @Override
    public String getName() {
        return NAME;
    }

    public PresenceStanzaType getPresenceType() {
        String type = getType();
        if (type == null)
            return null;
        return PresenceStanzaType.valueOfOrNull(type);
    }

    /**
     * show text must be from set {away, chat, dnd, xa} for not-extended namespaces
     * @return
     * @throws XMLSemanticError
     */
    public String getShow() throws XMLSemanticError {
        XMLElement element = getSingleInnerElementsNamed("show");
        if (element == null)
            return null; // show is optional, see RFC3921/2.2.2.1
        return element.getSingleInnerText().getText();
    }

    /**
     *
     * @param lang
     * @return
     * @throws XMLSemanticError - if langauge attributes are not unique, RFC3921/2.2.2.2
     */
    public String getStatus(String lang) throws XMLSemanticError {
        XMLElement element = getStatusMap().get(lang);
        if (element == null)
            return null;
        return element.getSingleInnerText().getText();
    }

    /**
     * @return all body elements, keyed by their lang attribute
     * @throws XMLSemanticError
     */
    public Map<String, XMLElement> getStatusMap() throws XMLSemanticError {
        return getInnerElementsByXMLLangNamed("status");
    }

    public int getPriority() throws XMLSemanticError {
        int priorityValue = 0; // priority default is 0, see RFC3921/2.2.2.3
        XMLElement element = getSingleInnerElementsNamed("priority");
        if (element != null) { // priority is optional, see RFC3921/2.2.2.3
            String priorityString = element.getSingleInnerText().getText();
            int intValue;
            try {
                intValue = Integer.parseInt(priorityString);
            } catch (NumberFormatException e) {
                throw new XMLSemanticError("presence priority must be an integer value in the -128 to 127 range", e);
            }
            if (intValue < -128 || intValue > 127) {
                throw new XMLSemanticError("presence priority must be an integer value in the -128 to 127 range");
            }
            priorityValue = intValue;
        }
        return priorityValue;
    }

    public int getPrioritySafe() {
        try {
            return getPriority();
        } catch (XMLSemanticError xmlSemanticError) {
            return 0; // default
        }
    }
}
