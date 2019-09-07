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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.DefaultUserArchiveBehaviour;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchivePreferences;

/**
 * @author Réda Housni Alaoui
 */
class UserMessageArchivePreferencesElement {

    static final String NAME = "prefs";

    private UserMessageArchivePreferencesElement() {
    }

    static XMLElement toXml(String namespace, UserMessageArchivePreferences preferences) {
        List<XMLFragment> alwaysJids = preferences.getAlwaysArchivedToOrFromJids().stream()
                .map(entity -> new XMLElement(null, "jid", null, Collections.emptyList(),
                        Collections.singletonList(new XMLText(entity.getFullQualifiedName()))))
                .collect(Collectors.toList());
        XMLElement alwaysElement = new XMLElement(null, "always", null, Collections.emptyList(), alwaysJids);

        List<XMLFragment> neverJids = preferences.getNeverArchivedToOrFromJids().stream()
                .map(entity -> new XMLElement(null, "jid", null, Collections.emptyList(),
                        Collections.singletonList(new XMLText(entity.getFullQualifiedName()))))
                .collect(Collectors.toList());
        XMLElement neverElement = new XMLElement(null, "never", null, Collections.emptyList(), neverJids);

        List<XMLFragment> prefsFragments = new ArrayList<>();
        prefsFragments.add(alwaysElement);
        prefsFragments.add(neverElement);

        Attribute defaultBehaviour = new Attribute("default", preferences.getDefaultBehaviour().name().toLowerCase());
        return new XMLElement(namespace, NAME, null, Collections.singletonList(defaultBehaviour), prefsFragments);
    }

    static UserMessageArchivePreferences fromXml(XMLElement prefsElement) throws XMLSemanticError {
        DefaultUserArchiveBehaviour defaultbehaviour = DefaultUserArchiveBehaviour
                .valueOf(prefsElement.getAttributeValue("default").toUpperCase());

        Set<Entity> alwaysJids = parseJids(prefsElement, "always");
        Set<Entity> neverJids = parseJids(prefsElement, "never");

        return new SimpleUserMessageArchivePreferences(defaultbehaviour, alwaysJids, neverJids);
    }

    private static Set<Entity> parseJids(XMLElement prefsElement, String parentElementName) throws XMLSemanticError {
        XMLElement parent = prefsElement.getSingleInnerElementsNamed(parentElementName);

        if (parent == null) {
            return Collections.emptySet();
        }

        return parent.getInnerElementsNamed("jid").stream().map(XMLElement::getInnerText).map(XMLText::getText)
                .map(EntityImpl::parseUnchecked).collect(Collectors.toSet());
    }
}
