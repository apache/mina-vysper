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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.AbstractInviteDecline;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Decline;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Invite;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Password;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

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
        if (xElement != null && xElement.getName().equals("x")
                && xElement.getNamespaceURI().equals(NamespaceURIs.XEP0045_MUC)) {
            // got x element and in the correct namespace
            return true;
        } else if (xElement != null && xElement.getNamespaceURI().length() == 0) {
            // no extension namespace, ok
            return true;
        } else if (xElement == null) {
            return true;
        } else {
            return false;
        }
    }

    public static Stanza createErrorStanza(String stanzaName, String namespaceUri, Entity from, Entity to, String id,
            String type, String errorName, List<XMLElement> innerElements) {
        //        <presence
        //        from='darkcave@chat.shakespeare.lit'
        //        to='hag66@shakespeare.lit/pda'
        //        type='error'>
        //      <error type='modify'>
        //        <jid-malformed xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
        //      </error>
        //    </presence>

        StanzaBuilder builder = new StanzaBuilder(stanzaName, namespaceUri);
        builder.addAttribute("from", from.getFullQualifiedName());
        builder.addAttribute("to", to.getFullQualifiedName());
        if (id != null)
            builder.addAttribute("id", id);
        builder.addAttribute("type", "error");

        if (innerElements != null) {
            for (XMLElement innerElement : innerElements) {
                builder.addPreparedElement(innerElement);
            }
        }

        builder.startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", type);
        builder.startInnerElement(errorName, NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
        builder.endInnerElement();

        return builder.build();
    }

    public static Stanza createErrorReply(Stanza originalStanza, StanzaErrorType type, StanzaErrorCondition error) {
        StanzaBuilder builder = new StanzaBuilder(originalStanza.getName(), originalStanza.getNamespaceURI());
        builder.addAttribute("from", originalStanza.getTo().getFullQualifiedName());
        builder.addAttribute("to", originalStanza.getFrom().getFullQualifiedName());
        builder.addAttribute("id", originalStanza.getAttributeValue("id"));
        builder.addAttribute("type", "error");

        for (XMLElement inner : originalStanza.getInnerElements()) {
            builder.addPreparedElement(inner);
        }

        builder.startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", type.value());
        builder.startInnerElement(error.value(), NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).endInnerElement();
        builder.endInnerElement();

        return builder.build();
    }

    public static Stanza createInviteMessageStanza(Stanza original, String password) throws EntityFormatException {
        X orginalX = X.fromStanza(original);

        Invite invite = orginalX.getInvite();
        if (invite == null || invite.getTo() == null) {
            throw new IllegalArgumentException("Invalid invite element, must exist and contain to attribute");
        }

        Invite newInvite = new Invite(original.getFrom(), null, invite.getReason());
        return createInviteDeclineMessageStanza(original, invite.getTo(), password, newInvite);
    }

    public static Stanza createDeclineMessageStanza(Stanza original) throws EntityFormatException {
        X orginalX = X.fromStanza(original);

        Decline decline = orginalX.getDecline();
        if (decline == null || decline.getTo() == null) {
            throw new IllegalArgumentException("Invalid decline element, must exist and contain to attribute");
        }

        Decline newDecline = new Decline(original.getFrom(), null, decline.getReason());
        return createInviteDeclineMessageStanza(original, decline.getTo(), null, newDecline);
    }

    public static Stanza createInviteDeclineMessageStanza(Stanza original, Entity to, String password,
            AbstractInviteDecline invdec) throws EntityFormatException {
        StanzaBuilder builder = StanzaBuilder.createMessageStanza(original.getTo(), to, null, null);
        List<XMLElement> inner = new ArrayList<XMLElement>();
        inner.add(invdec);
        if (password != null) {
            inner.add(new Password(password));
        }

        X newX = new X(NamespaceURIs.XEP0045_MUC_USER, inner);
        builder.addPreparedElement(newX);
        return builder.build();
    }

}
