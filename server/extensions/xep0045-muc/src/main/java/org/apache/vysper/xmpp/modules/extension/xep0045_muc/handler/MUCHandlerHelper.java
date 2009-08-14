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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCHandlerHelper {

    public static boolean verifyNamespace(Stanza stanza) {
        // either, the stanza should have a x element with the MUC namespace. Or, no extension 
        // element at all. Else, return false
        
        XMLElement xElement = stanza.getFirstInnerElement();
        if(xElement != null && xElement.getName().equals("x") 
                && xElement.getNamespaceURI().equals(NamespaceURIs.XEP0045_MUC)) {
            // got x element and in the correct namespace
            return true;
        } else if(xElement != null && xElement.getNamespaceURI() == null) {
            // no extension namespace, ok
            return true;
        } else if(xElement == null) {
            return true;
        } else {
            return false;
        }
    }
    
    public static Stanza createErrorStanza(String stanzaName, Entity from, Entity to, String type, String errorName, List<XMLElement> innerElements) {
        //        <presence
        //        from='darkcave@chat.shakespeare.lit'
        //        to='hag66@shakespeare.lit/pda'
        //        type='error'>
        //      <error type='modify'>
        //        <jid-malformed xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
        //      </error>
        //    </presence>

        StanzaBuilder builder = new StanzaBuilder(stanzaName);
        builder.addAttribute("from", from.getFullQualifiedName());
        builder.addAttribute("to", to.getFullQualifiedName());
        builder.addAttribute("type", "error");
        
        if(innerElements != null) {
            for(XMLElement innerElement : innerElements) {
                   builder.addPreparedElement(innerElement);
            }
        }
        
        builder.startInnerElement("error").addAttribute("type", type);
        builder.startInnerElement(errorName, NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
        builder.endInnerElement();
        
        return PresenceStanza.getWrapper(builder.getFinalStanza());
    }
}
