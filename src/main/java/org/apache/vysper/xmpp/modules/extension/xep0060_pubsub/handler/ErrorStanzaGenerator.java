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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * The ErrorStanzaGenerator is used to unify the creation of error stanzas across the pubsub
 * module.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class ErrorStanzaGenerator {
    
    /**
     * Creates a "JID malformed" error stanza (not specific to the pubsub module).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param iqStanzaID the staza ID of the errorneous request.
     * @return the generated stanza.
     */
    public Stanza generateJIDMalformedErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
        StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
        error.startInnerElement("error");
        error.addAttribute("type", "modify");
        error.startInnerElement("jid-malformed", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        error.endInnerElement(); // jid-malformed
        error.endInnerElement(); // error
        return error.getFinalStanza();
    }

    /**
     * Creates a "forbidden" error stanza (not specific to the pubsub module).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param iqStanzaID the staza ID of the errorneous request.
     * @return the generated stanza.
     */
    public Stanza generateInsufficientPrivilegesErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
        StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
        error.startInnerElement("error");
        error.addAttribute("type", "auth");
        error.startInnerElement("forbidden", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        error.endInnerElement(); // forbidden
        error.endInnerElement(); // error
        return error.getFinalStanza();
    }

    /**
     * Create the "no such subscriber" error stanza. This is a combination of "unexpected-request" and
     * "not-subscribed" error conditions (specific to pubsub).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param iqStanzaID the staza ID of the errorneous request.
     * @return the generated stanza.
     */
    public Stanza generateNoSuchSubscriberErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
        StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
        error.startInnerElement("error");
        error.addAttribute("type", "cancel");
        error.startInnerElement("unexpected-request", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        error.endInnerElement(); // unexpected-request
        error.startInnerElement("not-subscribed", NamespaceURIs.XEP0060_PUBSUB_ERRORS);
        error.endInnerElement(); // not-subscribed
        error.endInnerElement(); // error
        return error.getFinalStanza();
    }

    /**
     * Create the "SubID required" error stanza. This is a combination of "bad-request" and
     * "subid-required" error conditions (specific to pubsub).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param iqStanzaID the staza ID of the errorneous request.
     * @return the generated stanza.
     */
    public Stanza generateSubIDRequiredErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
        StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
        error.startInnerElement("error");
        error.addAttribute("type", "modify");
        error.startInnerElement("bad-request", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        error.endInnerElement(); // bad-request
        error.startInnerElement("subid-required", NamespaceURIs.XEP0060_PUBSUB_ERRORS);
        error.endInnerElement(); // subid-required
        error.endInnerElement(); // error
        return error.getFinalStanza();
    }

    /**
     * Create the "no such node" error stanza (not specific to the pubsub module).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param iqStanzaID the staza ID of the errorneous request.
     * @return the generated stanza.
     */
    public Stanza generateNoNodeErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
        StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
        error.startInnerElement("error");
        error.addAttribute("type", "cancel");
        error.startInnerElement("item-does-not-exist", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        error.endInnerElement(); // item-does-not-exist
        error.endInnerElement(); // error
        return error.getFinalStanza();
    }

    /**
     * Create the "SubID not valid" error stanza. This is a combination of "not-acceptable" and
     * "invalid-subid" error conditions (specific to pubsub).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param iqStanzaID the staza ID of the errorneous request.
     * @return the generated stanza.
     */
    public Stanza generateSubIDNotValidErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
        StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
        error.startInnerElement("error");
        error.addAttribute("type", "modify");
        error.startInnerElement("not-acceptable", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        error.endInnerElement(); // not-acceptable
        error.startInnerElement("invalid-subid", NamespaceURIs.XEP0060_PUBSUB_ERRORS);
        error.endInnerElement(); // invlaid-subid
        error.endInnerElement(); // error
        return error.getFinalStanza();
    }

    /**
     * Create the "JID don't match" error stanza. This is a combination of "bad-request" and
     * "invalid-jid" error conditions (specific to pubsub).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param iqStanzaID the staza ID of the errorneous request.
     * @return the generated stanza.
     */
    public Stanza generateJIDDontMatchErrorStanza(Entity sender, Entity receiver, String iqStanzaID) {
        StanzaBuilder error = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.ERROR, iqStanzaID);
        error.startInnerElement("error");
        error.addAttribute("type", "modify");
        error.startInnerElement("bad-request", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        error.endInnerElement(); // bad-request
        error.startInnerElement("invalid-jid", NamespaceURIs.XEP0060_PUBSUB_ERRORS);
        error.endInnerElement(); // invalid-jid
        error.endInnerElement(); // error
        return error.getFinalStanza();
    }
}
