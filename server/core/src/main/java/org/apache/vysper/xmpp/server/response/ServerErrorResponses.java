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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.modules.core.sasl.SASLFailureType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServerErrorResponses {

    private ServerErrorResponses() {
        // empty
    }

    public static Stanza getStreamError(StreamErrorCondition definedErrorCondition, String languageCode,
            String descriptiveText, XMLElement applicationSpecificError) {

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

        if (languageCode == null)
            languageCode = "en_US";
        StanzaBuilder stanzaBuilder = new StanzaBuilder("error", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STREAMS);

        stanzaBuilder.startInnerElement(definedErrorCondition.value(),
                NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STREAMS).endInnerElement();

        if (descriptiveText != null) {
            stanzaBuilder.startInnerElement("text", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STREAMS).addAttribute(
                    NamespaceURIs.XML, "lang", languageCode).addText(descriptiveText).endInnerElement();
        }

        if (applicationSpecificError != null) {
            stanzaBuilder.addPreparedElement(applicationSpecificError);
        }

        return stanzaBuilder.build();
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
    public static Stanza getStanzaError(StanzaErrorCondition errorCondition, XMPPCoreStanza stanza, StanzaErrorType type,
            String errorText, String errorLang, XMLElement errorConditionElement) {

        if (stanza != null && "error".equals(stanza.getType())) {
            return ServerErrorResponses.getStreamError(StreamErrorCondition.UNSUPPORTED_STANZA_TYPE,
                    errorLang, "cannot respond to IQ stanza of type error with the same", null);
        }

        StanzaBuilder responseBuilder = StanzaBuilder.createDirectReply(stanza, true, "error");

        fillErrorStanza(stanza, type, errorCondition, -1, errorText, errorLang, errorConditionElement, responseBuilder);

        return responseBuilder.build();
    }

    public static Stanza getStanzaError(StanzaErrorCondition errorCondition, XMPPCoreStanza stanza, StanzaErrorType type, int code,
            String errorText, String errorLang, XMLElement errorConditionElement) {
        
        if (stanza != null && "error".equals(stanza.getType())) {
            return ServerErrorResponses.getStreamError(StreamErrorCondition.UNSUPPORTED_STANZA_TYPE,
                    errorLang, "cannot respond to IQ stanza of type error with the same", null);
        }
        
        StanzaBuilder responseBuilder = StanzaBuilder.createDirectReply(stanza, true, "error");
        
        fillErrorStanza(stanza, type, errorCondition, code, errorText, errorLang, errorConditionElement, responseBuilder);
        
        return responseBuilder.build();
    }

    private static void fillErrorStanza(XMPPCoreStanza stanza, StanzaErrorType type, StanzaErrorCondition errorCondition,
            int code, String errorText, String errorLang, XMLElement errorConditionElement, StanzaBuilder responseBuilder) {
        // inline incoming stanza as of RFC 3920 9.3.1
        for (XMLElement innerElement : stanza.getInnerElements()) {
            responseBuilder.addPreparedElement(innerElement);
        }

        // error element
        responseBuilder.startInnerElement("error", NamespaceURIs.JABBER_CLIENT).addAttribute("type", type.value());
        if(code != -1) responseBuilder.addAttribute("code", Integer.toString(code));
        
        
        // insert defined error condition relating to the stanza error type
        responseBuilder.startInnerElement(errorCondition.value(), NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS);
        responseBuilder.endInnerElement();

        // optional error text
        if (errorText != null && errorLang != null) {
            responseBuilder.startInnerElement("text", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS).addAttribute(
                    NamespaceURIs.XML, "lang", errorLang).addText(errorText);
        }
        // optional application specific error condition element
        if (errorConditionElement != null)
            responseBuilder.addPreparedElement(errorConditionElement);

        responseBuilder.endInnerElement();
    }

    public static Stanza getTLSFailure() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("failure", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
        return stanzaBuilder.build();
    }

    public static Stanza getSASLFailure(SASLFailureType failureType) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("failure", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        if (failureType != null) {
            stanzaBuilder.startInnerElement(failureType.toString(), NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)
                    .endInnerElement();
        }
        return stanzaBuilder.build();
    }
}
