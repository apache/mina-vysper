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

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * <x xmlns='http://jabber.org/protocol/muc#user'>
 *   <item affiliation='none' role='none'>
 *     <actor jid='fluellen@shakespeare.lit'/>
 *     <reason>Avaunt, you cullion!</reason>
 *   </item>
 *   <status code='307'/>
 * </x>
 *
 */
public class UserX extends XMLElement {

    public UserX(XMLElement...elements) {
        this(Arrays.asList(elements));
    }

    public UserX(List<XMLElement> elements) {
        super(NamespaceURIs.XEP0045_MUC_USER, "x", null, null, elements.toArray(new XMLElement[]{}));
    }
    
    public Invite getInvite() {
        try {
            XMLElement inviteElm = getSingleInnerElementsNamed("invite");
            if(inviteElm != null) {
                return new Invite(inviteElm);
            } else {
                return null;
            }
        } catch (XMLSemanticError e) {
            throw new IllegalArgumentException("Invalid stanza", e);
        }
    }

    public Decline getDecline() {
        try {
            XMLElement inviteElm = getSingleInnerElementsNamed("decline");
            if(inviteElm != null) {
                return new Decline(inviteElm);
            } else {
                return null;
            }
        } catch (XMLSemanticError e) {
            throw new IllegalArgumentException("Invalid stanza", e);
        }
    }

    
    public Password getPassword() {
        try {
            XMLElement passwordElm = getSingleInnerElementsNamed("password");
            if(passwordElm != null && passwordElm.getInnerText() != null) {
                return new Password(passwordElm.getInnerText().getText());
            } else {
                return null;
            }
        } catch (XMLSemanticError e) {
            throw new IllegalArgumentException("Invalid stanza", e);
        }
    }
    
    public String getPasswordValue() {
        Password password = getPassword();
        if(password != null && password.getInnerText() != null) {
            return password.getInnerText().getText();
        } else {
            return null;
        }
    }

    
}
