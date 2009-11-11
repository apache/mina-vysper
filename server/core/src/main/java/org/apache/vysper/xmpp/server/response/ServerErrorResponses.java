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

package org.apache.vysper.xmpp.server.response;

import org.apache.vysper.xmpp.modules.core.sasl.SASLFailureType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServerErrorResponses
{
    private static ServerErrorResponses serverErrorResponsesInstance = null;

    public static ServerErrorResponses getInstance() {
        if (serverErrorResponsesInstance == null) {
            serverErrorResponsesInstance = new ServerErrorResponses();
        }
        return serverErrorResponsesInstance;
    }

    protected ServerErrorResponses() {
        // empty
    }

    public Stanza getStreamError(StreamErrorCondition definedErrorCondition, String languageCode, String descriptiveText, XMLElement applicationSpecificError) {

        /*
           <stream:jabber>
             <defined-condition xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>
             <text xmlns='urn:ietf:params:xml:ns:xmpp-streams'
                   xml:lang='langcode'>
               OPTIONAL descriptive text
             </text>
             [OPTIONAL application-specific condition element]
           </stream:jabber>
        */

        if (languageCode == null) languageCode = "en_US";
        StanzaBuilder stanzaBuilder = new StanzaBuilder("error");

        stanzaBuilder.startInnerElement(definedErrorCondition.value())
            .addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STREAMS)
            .endInnerElement();

        if (descriptiveText != null) {
            stanzaBuilder.startInnerElement("text")
                .addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STREAMS)
                .addAttribute(NamespaceURIs.XML, "lang", languageCode)
                .addText(descriptiveText)
                .endInnerElement();
        }

        if (applicationSpecificError != null) {
            stanzaBuilder.addPreparedElement(applicationSpecificError);
        }

        return stanzaBuilder.getFinalStanza();
    }

    /**
     * TODO move to a more general error handling
     * @param errorCondition - corresponds to one of the defined stanza error conditions
     * @param stanza - the stanza to which the error stanza is the answer
     * @param type
     * @param errorText - is optional together with errorLang, both together might be NULL
     * @param errorLang - must be present, if errorText is not NULL
     * @param errorConditionElement - optional application specific error condition element
     * @return error response stanza
     */
    public Stanza getStanzaError(StanzaErrorCondition errorCondition, XMPPCoreStanza stanza, StanzaErrorType type,
                                 String errorText, String errorLang,
                                 XMLElement errorConditionElement) {

        if (stanza != null && "error".equals(stanza.getType())) {
            return ServerErrorResponses.getInstance().getStreamError(StreamErrorCondition.UNSUPPORTED_STANZA_TYPE, errorLang,
                                                               "cannot respond to IQ stanza of type error with the same", null);
        }

        StanzaBuilder responseBuilder = StanzaBuilder.createDirectReply(stanza, true, "error");

        fillErrorStanza(stanza, type, errorCondition, errorText, errorLang, errorConditionElement, responseBuilder);

        return responseBuilder.getFinalStanza();
    }

    private void fillErrorStanza(XMPPCoreStanza stanza, StanzaErrorType type, StanzaErrorCondition errorCondition, String errorText, String errorLang, XMLElement errorConditionElement, StanzaBuilder responseBuilder) {
        // inline incoming stanza as of RFC 3920 9.3.1
        for(XMLElement innerElement : stanza.getInnerElements()) {
            responseBuilder.addPreparedElement(innerElement);
        }

        // error element
        responseBuilder.startInnerElement("error")
                         .addAttribute("type", type.value());

        // insert defined error condition relating to the stanza error type
        responseBuilder.startInnerElement(errorCondition.value());
        responseBuilder.addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        responseBuilder.endInnerElement();

        // optional error text
        if (errorText != null && errorLang != null) {
            responseBuilder.startInnerElement("text")
                             .addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS)
                             .addAttribute(NamespaceURIs.XML, "lang", errorLang)
                             .addText(errorText);
        }
        // optional application specific error condition element
        if (errorConditionElement != null) responseBuilder.addPreparedElement(errorConditionElement);

        responseBuilder.endInnerElement();
    }

    public Stanza getTLSFailure() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("failure");
        stanzaBuilder.addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
        return stanzaBuilder.getFinalStanza();
    }

    public Stanza getSASLFailure(SASLFailureType failureType) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("failure");
        stanzaBuilder.addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        if (failureType != null) {
            stanzaBuilder.startInnerElement(failureType.toString()).endInnerElement();
        }
        return stanzaBuilder.getFinalStanza();
    }
}
