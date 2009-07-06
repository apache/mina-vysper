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
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.NamespaceAttribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;

/**
 * The ErrorStanzaGenerator is used to unify the creation of error stanzas across the pubsub
 * module.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class ErrorStanzaGenerator {
    
    // constants for pubsub related error elements
    protected static final String NOT_SUBSCRIBED = "not-subscribed";
    protected static final String SUBID_REQUIRED = "subid-required";
    protected static final String INVALID_SUBID = "invalid-subid";
    protected static final String INVALID_JID = "invalid-jid";
    
    // The ServerErrorResponses object for generating type-safe error stanzas
    protected ServerErrorResponses errorResponses = ServerErrorResponses.getInstance();
    
    /**
     * Creates a "JID malformed" error stanza (not specific to the pubsub module).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param stanza the stanza of the erroneous request.
     * @return the generated stanza.
     */
    public Stanza generateJIDMalformedErrorStanza(Entity sender, Entity receiver, IQStanza stanza) {
        return errorResponses.getStanzaError(StanzaErrorCondition.JID_MALFORMED, stanza, StanzaErrorType.MODIFY, null, null, null);
    }

    /**
     * Creates a "forbidden" error stanza (not specific to the pubsub module).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param stanza the stanza of the erroneous request.
     * @return the generated stanza.
     */
    public Stanza generateInsufficientPrivilegesErrorStanza(Entity sender, Entity receiver, IQStanza stanza) {
        return errorResponses.getStanzaError(StanzaErrorCondition.FORBIDDEN, stanza, StanzaErrorType.AUTH, null, null, null);
    }

    /**
     * Create the "no such subscriber" error stanza. This is a combination of "unexpected-request" and
     * "not-subscribed" error conditions (specific to pubsub).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param stanza the stanza of the erroneous request.
     * @return the generated stanza.
     */
    public Stanza generateNoSuchSubscriberErrorStanza(Entity sender, Entity receiver, IQStanza stanza) {
        XMLElement notSubscribed = createXMLElement(NOT_SUBSCRIBED, NamespaceURIs.XEP0060_PUBSUB_ERRORS);
        return errorResponses.getStanzaError(StanzaErrorCondition.UNEXPECTED_REQUEST, stanza, StanzaErrorType.CANCEL, null, null, notSubscribed);
    }

    /**
     * Create the "SubID required" error stanza. This is a combination of "bad-request" and
     * "subid-required" error conditions (specific to pubsub).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param stanza the stanza of the erroneous request.
     * @return the generated stanza.
     */
    public Stanza generateSubIDRequiredErrorStanza(Entity sender, Entity receiver, IQStanza stanza) {
        XMLElement subidRequired = createXMLElement(SUBID_REQUIRED, NamespaceURIs.XEP0060_PUBSUB_ERRORS);
        return errorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza, StanzaErrorType.MODIFY, null, null, subidRequired);
    }

    /**
     * Create the "no such node" error stanza (not specific to the pubsub module).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param stanza the stanza of the erroneous request.
     * @return the generated stanza.
     */
    public Stanza generateNoNodeErrorStanza(Entity sender, Entity receiver, IQStanza stanza) {
        return errorResponses.getStanzaError(StanzaErrorCondition.ITEM_NOT_FOUND, stanza, StanzaErrorType.CANCEL, null, null, null);
    }

    /**
     * Create the "SubID not valid" error stanza. This is a combination of "not-acceptable" and
     * "invalid-subid" error conditions (specific to pubsub).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param stanza the stanza of the erroneous request.
     * @return the generated stanza.
     */
    public Stanza generateSubIDNotValidErrorStanza(Entity sender, Entity receiver, IQStanza stanza) {
        XMLElement invalidSubID = createXMLElement(INVALID_SUBID, NamespaceURIs.XEP0060_PUBSUB_ERRORS);
        return errorResponses.getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE, stanza, StanzaErrorType.MODIFY, null, null, invalidSubID);
    }

    /**
     * Create the "JID don't match" error stanza. This is a combination of "bad-request" and
     * "invalid-jid" error conditions (specific to pubsub).
     * 
     * @param sender who sent the erroneous request.
     * @param receiver who was the recipient of the erroneous request.
     * @param stanza the stanza of the erroneous request.
     * @return the generated stanza.
     */
    public Stanza generateJIDDontMatchErrorStanza(Entity sender, Entity receiver, IQStanza stanza) {
        XMLElement invalidJID = createXMLElement(INVALID_JID, NamespaceURIs.XEP0060_PUBSUB_ERRORS);
        return errorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza, StanzaErrorType.MODIFY, null, null, invalidJID);
    }

    /**
     * Create a conflict error stanza. For example if a node with an existing nodeID is to be created.
     * 
     * @param sender
     * @param receiver
     * @param stanza
     * @return
     */
    public Stanza generateDuplicateNodeErrorStanza(Entity sender, Entity receiver, IQStanza stanza) {       
        return errorResponses.getStanzaError(StanzaErrorCondition.CONFLICT, stanza, StanzaErrorType.CANCEL, null, null, null);
    }


    /**
     * Creates a single element lying within a certain default namespace.
     * 
     * @param elementName the name of the element
     * @param namespace the default namespace
     * @return the <elementName xmlns="namespace"/> element
     */
    private XMLElement createXMLElement(String elementName, String namespace) {
        XMLElement element = new XMLElement(elementName, null, new Attribute[] {new NamespaceAttribute(namespace)}, (XMLFragment[])null);
        return element;
    }

}
