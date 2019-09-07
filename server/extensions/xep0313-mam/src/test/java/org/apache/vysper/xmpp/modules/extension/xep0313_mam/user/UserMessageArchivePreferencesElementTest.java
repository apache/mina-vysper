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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.DefaultUserArchiveBehaviour;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchivePreferences;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class UserMessageArchivePreferencesElementTest {

    @Test
    public void toXml() {
        UserMessageArchivePreferences preferences = new SimpleUserMessageArchivePreferences(
                DefaultUserArchiveBehaviour.ROSTER, Collections.singleton(EntityImpl.parseUnchecked("always@foo.com")),
                Collections.singleton(EntityImpl.parseUnchecked("never@foo.com")));

        XMLElement prefs = UserMessageArchivePreferencesElement.toXml("foo", preferences);

        assertEquals("prefs", prefs.getName());
        assertEquals("foo", prefs.getNamespaceURI());

        List<XMLElement> innerElements = prefs.getInnerElements();

        XMLElement always = innerElements.get(0);
        assertEquals("always", always.getName());
        List<XMLElement> alwaysJids = always.getInnerElements();
        assertEquals(1, alwaysJids.size());
        assertEquals("always@foo.com", alwaysJids.get(0).getInnerText().getText());

        XMLElement never = innerElements.get(1);
        assertEquals("never", never.getName());
        List<XMLElement> neverJids = never.getInnerElements();
        assertEquals(1, neverJids.size());
        assertEquals("never@foo.com", neverJids.get(0).getInnerText().getText());
    }

    @Test
    public void fromXml() throws XMLSemanticError {
        UserMessageArchivePreferences preferences = new SimpleUserMessageArchivePreferences(
                DefaultUserArchiveBehaviour.ROSTER, Collections.singleton(EntityImpl.parseUnchecked("always@foo.com")),
                Collections.singleton(EntityImpl.parseUnchecked("never@foo.com")));

        XMLElement prefsElement = UserMessageArchivePreferencesElement.toXml("foo", preferences);

        UserMessageArchivePreferences parsedPreferences = UserMessageArchivePreferencesElement.fromXml(prefsElement);
        
        assertEquals(preferences.getDefaultBehaviour(), parsedPreferences.getDefaultBehaviour());
        assertEquals(preferences.getAlwaysArchivedToOrFromJids().size(), parsedPreferences.getAlwaysArchivedToOrFromJids().size());
        assertEquals(preferences.getNeverArchivedToOrFromJids().size(), parsedPreferences.getNeverArchivedToOrFromJids().size());
    }

}