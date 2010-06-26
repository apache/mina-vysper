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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.datetime.DateTimeProfile;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;

public class History extends XMLElement {

    private static final String ELEMENT_HISTORY = "history";

    private static final String ATTRIBUTE_SINCE = "since";

    private static final String ATTRIBUTE_SECONDS = "seconds";

    private static final String ATTRIBUTE_MAXCHARS = "maxchars";

    private static final String ATTRIBUTE_MAXSTANZAS = "maxstanzas";

    public static History fromStanza(Stanza stanza) {
        // history is in a x element in the MUC namespace
        try {
            XMLElement xElm = stanza.getSingleInnerElementsNamed("x", NamespaceURIs.XEP0045_MUC);
            if (xElm != null) {
                XMLElement historyElm = xElm.getSingleInnerElementsNamed("history");
                if (historyElm != null) {
                    return new History(historyElm);
                }
            }

            // history element not found
            return null;
        } catch (XMLSemanticError e) {
            throw new IllegalArgumentException("Invalid stanza", e);
        }
    }

    public History(XMLElement elm) {
        super(NamespaceURIs.XEP0045_MUC, ELEMENT_HISTORY, null, elm.getAttributes(), null);
    }

    public History(Integer maxstanzas, Integer maxchars, Integer seconds, Calendar since) {
        super(NamespaceURIs.XEP0045_MUC, ELEMENT_HISTORY, null, createAttributes(maxstanzas, maxchars, seconds, since),
                null);
    }

    private static List<Attribute> createAttributes(Integer maxstanzas, Integer maxchars, Integer seconds,
            Calendar since) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        if (maxstanzas != null)
            attributes.add(new Attribute(ATTRIBUTE_MAXSTANZAS, maxstanzas.toString()));
        if (maxchars != null)
            attributes.add(new Attribute(ATTRIBUTE_MAXCHARS, maxchars.toString()));
        if (seconds != null)
            attributes.add(new Attribute(ATTRIBUTE_SECONDS, seconds.toString()));
        if (since != null)
            attributes.add(new Attribute(ATTRIBUTE_SINCE, DateTimeProfile.getInstance().getDateTimeInUTC(
                    since.getTime())));
        return attributes;
    }

    private Integer getAttributeIntValue(String name) {
        String value = getAttributeValue(name);
        if (value != null && value.trim().length() > 0) {
            return Integer.valueOf(value);
        } else {
            return null;
        }
    }

    public Integer getMaxStanzas() {
        return getAttributeIntValue(ATTRIBUTE_MAXSTANZAS);
    }

    public Integer getMaxChars() {
        return getAttributeIntValue(ATTRIBUTE_MAXCHARS);
    }

    public Integer getSeconds() {
        return getAttributeIntValue(ATTRIBUTE_SECONDS);
    }

    public Calendar getSince() {
        String value = getAttributeValue(ATTRIBUTE_SINCE);
        if (value != null) {
            // TODO handle IllegalArgumentException
            Calendar timestamp = DateTimeProfile.getInstance().fromDateTime(value);
            return timestamp;
        } else {
            return null;
        }
    }
}
