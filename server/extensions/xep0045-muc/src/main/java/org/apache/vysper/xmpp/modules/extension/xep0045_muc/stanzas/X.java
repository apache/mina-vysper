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

import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.NamespaceAttribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;

public class X extends XMLElement {

    public static X fromStanza(Stanza stanza) {
        try {
            XMLElement xElm = stanza.getSingleInnerElementsNamed("x", NamespaceURIs.XEP0045_MUC);
            if(xElm != null) {
                return new X(xElm.getInnerElements());
            } else {
                return null;
            }
        } catch (XMLSemanticError e) {
            throw new IllegalArgumentException("Invalid stanza", e);
        }
    }
    
    public X(XMLElement...elements) {
        super("x", null, new Attribute[]{
            new NamespaceAttribute(NamespaceURIs.XEP0045_MUC)}, elements);
    }
    
    public X(List<XMLElement> elements) {
        super("x", null, new Attribute[]{
            new NamespaceAttribute(NamespaceURIs.XEP0045_MUC)}, elements.toArray(new XMLElement[]{}));
    }

    public String getPassword() {
        try {
            XMLElement passwordElm = getSingleInnerElementsNamed("password");
            if(passwordElm != null && passwordElm.getInnerText() != null) {
                return passwordElm.getInnerText().getText();
            } else {
                return null;
            }
        } catch (XMLSemanticError e) {
            throw new IllegalArgumentException("Invalid stanza", e);
        }
    }
    

    
}
