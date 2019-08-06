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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.query;

import static java.util.Optional.ofNullable;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.datetime.DateTimeProfile;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.DateTimeFilter;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormParser;

/**
 * @author Réda Housni Alaoui
 */
public class X implements DateTimeFilter {

    public static final String ELEMENT_NAME = "x";

    private final Map<String, Object> fields;

    public X(String namespace, XMLElement element) {
        if (!ELEMENT_NAME.equals(element.getName())) {
            throw new IllegalArgumentException(
                    "Query element must be named '" + ELEMENT_NAME + "' instead of '" + element.getName() + "'");
        }
        if (!NamespaceURIs.JABBER_X_DATA.equals(element.getNamespaceURI())) {
            throw new IllegalArgumentException("Query element must be bound to namespace uri '" + namespace
                    + "' instead of '" + element.getNamespaceURI() + "'");
        }
        fields = new DataFormParser(element).extractFieldValues();
    }

    public static X empty(String namespace) {
        return new X(namespace, new XMLElement(NamespaceURIs.JABBER_X_DATA, ELEMENT_NAME, null, Collections.emptyList(),
                Collections.emptyList()));
    }

    private Optional<String> getStringValue(String fieldName) {
        return ofNullable(fields.get(fieldName)).map(String.class::cast);
    }

    private Optional<ZonedDateTime> getZonedDateTime(String fieldName) {
        return getStringValue(fieldName).map(stringDate -> DateTimeProfile.getInstance().fromZonedDateTime(stringDate));
    }

    public Optional<Entity> getWith() {
        return getStringValue("with").map(EntityImpl::parseUnchecked);
    }

    public Optional<ZonedDateTime> start() {
        return getZonedDateTime("start");
    }

    public Optional<ZonedDateTime> end() {
        return getZonedDateTime("end");
    }

}
