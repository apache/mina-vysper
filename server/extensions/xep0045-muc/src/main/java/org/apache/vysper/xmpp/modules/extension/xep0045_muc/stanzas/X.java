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

import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;

public class X extends XMLElement {

    public static X fromStanza(Stanza stanza) {
        List<XMLElement> xElms = stanza.getInnerElementsNamed("x");
        XMLElement xElm = null;
        // find an element with one of the MUC namespaces
        for (XMLElement elm : xElms) {
            if (elm.getNamespaceURI() != null && elm.getNamespaceURI().startsWith(NamespaceURIs.XEP0045_MUC)) {
                xElm = elm;
                break;
            }
        }
        if (xElm != null) {
            return new X(xElm.getInnerElements());
        } else {
            return null;
        }
    }

    public X(XMLElement... elements) {
        this(NamespaceURIs.XEP0045_MUC, elements);
    }

    public X(String ns, XMLElement... elements) {
        super(ns, "x", null, null, elements);
    }

    public X(List<XMLElement> elements) {
        this(NamespaceURIs.XEP0045_MUC, elements);
    }

    public X(String ns, List<XMLElement> elements) {
        super(ns, "x", null, null, elements.toArray(new XMLElement[] {}));
    }

    public Invite getInvite() {
        try {
            XMLElement inviteElm = getSingleInnerElementsNamed("invite");
            if (inviteElm != null) {
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
            if (inviteElm != null) {
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
            if (passwordElm != null && passwordElm.getInnerText() != null) {
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
        if (password != null && password.getInnerText() != null) {
            return password.getInnerText().getText();
        } else {
            return null;
        }
    }

}
