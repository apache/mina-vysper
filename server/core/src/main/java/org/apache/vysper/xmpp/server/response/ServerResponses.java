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

import java.util.List;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServerResponses {

    public Stanza getStreamOpenerForError(boolean forClient, Entity from, XMPPVersion version, Stanza errorStanza) {
        return getStreamOpener(forClient, from, null, version, errorStanza).build();
    }

    public Stanza getStreamOpenerForClient(Entity from, XMPPVersion version, SessionContext sessionContext) {
        Stanza innerFeatureStanza;
        if (sessionContext.getState() == SessionState.INITIATED)
            innerFeatureStanza = getFeaturesForEncryption(sessionContext);
        else if (sessionContext.getState() == SessionState.ENCRYPTED)
            innerFeatureStanza = getFeaturesForAuthentication(sessionContext.getServerRuntimeContext()
                    .getServerFeatures().getAuthenticationMethods());
        else if (sessionContext.getState() == SessionState.AUTHENTICATED) {
            sessionContext.setIsReopeningXMLStream();
            innerFeatureStanza = getFeaturesForSession();
        } else {
            throw new IllegalStateException("unsupported state for responding with stream opener");
        }

        StanzaBuilder stanzaBuilder = getStreamOpener(true, from, sessionContext.getXMLLang(), version,
                sessionContext.getSessionId(), innerFeatureStanza);

        return stanzaBuilder.build();
    }

    public Stanza getStreamOpenerForServerAcceptor(Entity from, XMPPVersion version, SessionContext sessionContext, boolean tlsConfigured) {
        
        XMLElement features = null;
        
        // only include <features> if the other server support version 1.0
        if(XMPPVersion.VERSION_1_0.equals(version)) {
            XMLElementBuilder featureBuilder = new XMLElementBuilder("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS);
            if (sessionContext.getState() == SessionState.INITIATED) {
                if(tlsConfigured) {
                    featureBuilder.startInnerElement("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS).endInnerElement();
                }
                featureBuilder.startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement();
                
            } else if (sessionContext.getState() == SessionState.ENCRYPTED) {
                featureBuilder.startInnerElement("dialback", NamespaceURIs.URN_XMPP_FEATURES_DIALBACK).endInnerElement();
            } else {
                throw new IllegalStateException("unsupported state for responding with stream opener");
            }
            features = featureBuilder.build();
        }
        
        StanzaBuilder stanzaBuilder = getStreamOpener(false, from, sessionContext.getXMLLang(), version,
                sessionContext.getSessionId(), features);

        stanzaBuilder.declareNamespace("db", NamespaceURIs.JABBER_SERVER_DIALBACK);
        return stanzaBuilder.build();
    }

    public Stanza getStreamOpenerForServerConnector(Entity from, Entity to, XMPPVersion version, SessionContext sessionContext) {
        StanzaBuilder stanzaBuilder = getStreamOpener(false, from, sessionContext.getXMLLang(), version,
                null, null);
        stanzaBuilder.addAttribute("to", to.getDomain());
        stanzaBuilder.declareNamespace("db", NamespaceURIs.JABBER_SERVER_DIALBACK);
        return stanzaBuilder.build();
    }

    
    public StanzaBuilder getStreamOpener(boolean forClient, Entity from, String xmlLang, XMPPVersion version,
            Stanza innerStanza) {
        return getStreamOpener(forClient, from, xmlLang, version, null, innerStanza);
    }

    public StanzaBuilder getStreamOpener(boolean forClient, Entity from, String xmlLang, XMPPVersion version,
            String sessionId, XMLElement innerStanza) {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("stream", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS,
                "stream").declareNamespace("", forClient ? NamespaceURIs.JABBER_CLIENT : NamespaceURIs.JABBER_SERVER)
                .addAttribute("from", from.getFullQualifiedName());
        if (xmlLang != null)
            stanzaBuilder.addAttribute(NamespaceURIs.XML, "lang", xmlLang);
        if (version != null)
            stanzaBuilder.addAttribute("version", version.toString());
        if (sessionId != null)
            stanzaBuilder.addAttribute("id", sessionId);
        if (innerStanza != null)
            stanzaBuilder.addPreparedElement(innerStanza);
        return stanzaBuilder;
    }

    public Stanza getFeaturesForEncryption(SessionContext sessionContext) {

        StanzaBuilder stanzaBuilder = startFeatureStanza();
        stanzaBuilder.startInnerElement("starttls", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
        if (sessionContext.getServerRuntimeContext().getServerFeatures().isStartTLSRequired()) {
            stanzaBuilder.startInnerElement("required", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS)
                    .endInnerElement();
        }
        stanzaBuilder.endInnerElement();

        return stanzaBuilder.build();
    }

    public Stanza getFeaturesForAuthentication(List<SASLMechanism> authenticationMethods) {

        StanzaBuilder stanzaBuilder = startFeatureStanza();
        stanzaBuilder.startInnerElement("mechanisms", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        for (SASLMechanism authenticationMethod : authenticationMethods) {
            stanzaBuilder.startInnerElement("mechanism", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText(
                    authenticationMethod.getName()).endInnerElement();
        }
        stanzaBuilder.endInnerElement();

        return stanzaBuilder.build();
    }

    public Stanza getFeaturesForSession() {
        StanzaBuilder stanzaBuilder = startFeatureStanza();

        stanzaBuilder.startInnerElement("bind", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND).startInnerElement(
                "required", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND).endInnerElement();
        stanzaBuilder.endInnerElement();

        // session establishment is here for RFC3921 compatibility and is planed to be removed in revisions of this RFC.
        stanzaBuilder.startInnerElement("session", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SESSION)
                .startInnerElement("required", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SESSION).endInnerElement();
        stanzaBuilder.endInnerElement();

        return stanzaBuilder.build();
    }

    protected StanzaBuilder startFeatureStanza() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("features", NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS,
                "stream");

        return stanzaBuilder;
    }

    public Stanza getTLSProceed() {

        StanzaBuilder stanzaBuilder = new StanzaBuilder("proceed", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
        return stanzaBuilder.build();
    }

    public Stanza getAuthAborted() {

        StanzaBuilder stanzaBuilder = new StanzaBuilder("aborted", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
        return stanzaBuilder.build();
    }

}
