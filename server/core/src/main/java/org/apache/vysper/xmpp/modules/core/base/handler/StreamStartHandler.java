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

package org.apache.vysper.xmpp.modules.core.base.handler;

import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StreamStartHandler implements StanzaHandler {
    public String getName() {
        return "stream";
    }

    public boolean verify(Stanza stanza) {
        if (stanza == null)
            return false;
        if (!getName().equals(stanza.getName()))
            return false;
        String namespaceURI = stanza.getNamespaceURI();
        if (namespaceURI == null)
            return false;
        return namespaceURI.equals(NamespaceURIs.JABBER_CLIENT) || namespaceURI.equals(NamespaceURIs.JABBER_SERVER);
    }

    public boolean isSessionRequired() {
        return true;
    }

    public void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext, boolean isOutboundStanza,
            SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker) {
        XMLElementVerifier xmlElementVerifier = stanza.getVerifier();
        boolean jabberNamespace = NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS.equals(stanza.getNamespaceURI());

        boolean clientCall = xmlElementVerifier.namespacePresent(NamespaceURIs.JABBER_CLIENT);
        boolean serverCall = xmlElementVerifier.namespacePresent(NamespaceURIs.JABBER_SERVER);

        // TODO is it better to derive c2s or s2s from the type of endpoint and verify
        // the namespace here?
        if (clientCall && serverCall)
            serverCall = false; // silently ignore ambiguous attributes
        if (serverCall)
            sessionContext.setServerToServer();
        else
            sessionContext.setClientToServer();

        if (sessionStateHolder.getState() != SessionState.INITIATED
                && sessionStateHolder.getState() != SessionState.ENCRYPTED
                && sessionStateHolder.getState() != SessionState.AUTHENTICATED) {
            stanzaBroker.writeToSession(buildUnsupportedStanzaType("unexpected stream start"));
            return;
        }

        // http://etherx.jabber.org/streams cannot be omitted
        if (!jabberNamespace) {
            stanzaBroker.writeToSession(buildIllegalNamespaceError(
                    "namespace is mandatory: " + NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS));
            return;
        }

        // processing xml:lang
        String xmlLang = stanza.getXMLLang();
        sessionContext.setXMLLang(xmlLang);

        // processing version
        XMPPVersion responseVersion = null;
        // if version is not present, version "0.0" is assumed, represented by NULL.
        String versionAttributeValue = stanza.getAttributeValue("version");
        if (versionAttributeValue != null) {
            XMPPVersion clientVersion;
            try {
                clientVersion = new XMPPVersion(versionAttributeValue);
            } catch (IllegalArgumentException e) {
                // version string does not conform to spec
                stanzaBroker.writeToSession(
                        buildUnsupportedVersionError(xmlLang, versionAttributeValue, "illegal version value: "));
                return;
            }
            // check if version is supported
            if (!clientVersion.equals(XMPPVersion.VERSION_1_0)) {
                if (clientVersion.getMajor() == XMPPVersion.VERSION_1_0.getMajor()) {
                    // we live with the higher minor version, but only support ours
                    responseVersion = XMPPVersion.VERSION_1_0;
                } else {
                    // we do not support major changes, as of RFC3920
                    stanzaBroker.writeToSession(buildUnsupportedVersionError(xmlLang, versionAttributeValue,
                            "major version change not supported: "));
                    return;
                }
            } else {
                responseVersion = clientVersion;
            }
        }

        if (xmlElementVerifier.attributePresent("id")) {
            // ignore silently (see RFC3920 4.4)
        }

        Stanza responseStanza = null;
        if (clientCall) {
            // RFC3920: 'to' attribute SHOULD be used by the initiating entity
            String toValue = stanza.getAttributeValue("to");
            if (toValue != null) {
                try {
                    EntityImpl.parse(toValue);
                } catch (EntityFormatException e) {
                    stanzaBroker.writeToSession(ServerErrorResponses.getStreamError(
                            StreamErrorCondition.IMPROPER_ADDRESSING, sessionContext.getXMLLang(),
                            "could not parse incoming stanza's TO attribute", null));
                    return;
                }
                // TODO check if toEntity is served by this server
                // if (!server.doesServe(toEntity)) throw WhateverException();

                // TODO RFC3920: 'from' attribute SHOULD be silently ignored by the receiving
                // entity
                // TODO RFC3920bis: 'from' attribute SHOULD be not ignored by the receiving
                // entity and used as 'to' in responses
            }
            responseStanza = new ServerResponses().getStreamOpenerForClient(sessionContext.getServerJID(),
                    responseVersion, sessionContext);
        } else if (serverCall) {
            // RFC3920: 'from' attribute SHOULD be used by the receiving entity
            String fromValue = stanza.getAttributeValue("from");
            if (fromValue != null) {
                try {
                    EntityImpl.parse(fromValue);
                } catch (EntityFormatException e) {
                    stanzaBroker.writeToSession(ServerErrorResponses.getStreamError(StreamErrorCondition.INVALID_FROM,
                            sessionContext.getXMLLang(), "could not parse incoming stanza's FROM attribute", null));
                    return;
                }
            }

            responseStanza = new ServerResponses().getStreamOpenerForServerAcceptor(sessionContext.getServerJID(),
                    responseVersion, sessionContext, serverRuntimeContext.getSslContext() != null);
        } else {
            String descriptiveText = "one of the two namespaces must be present: " + NamespaceURIs.JABBER_CLIENT
                    + " or " + NamespaceURIs.JABBER_SERVER;
            stanzaBroker.writeToSession(buildIllegalNamespaceError(descriptiveText));
            return;
        }

        // if all is correct, go to next phase
        switch (sessionStateHolder.getState()) {

        case AUTHENTICATED:
        case ENCRYPTED:
            // do not change state!
            break;
        default:
            sessionStateHolder.setState(SessionState.STARTED);
        }

        if (responseStanza == null) {
            return;
        }
        stanzaBroker.writeToSession(responseStanza);
    }

    private Stanza buildIllegalNamespaceError(String descriptiveText) {
        return ServerErrorResponses.getStreamError(StreamErrorCondition.INVALID_NAMESPACE, null, descriptiveText, null);
    }

    private Stanza buildUnsupportedStanzaType(String descriptiveText) {
        return ServerErrorResponses.getStreamError(StreamErrorCondition.UNSUPPORTED_STANZA_TYPE, null, descriptiveText,
                null);
    }

    private Stanza buildUnsupportedVersionError(String xmlLang, String versionAttributeValue, String errorMessage) {
        if (xmlLang == null)
            xmlLang = "en_US";
        return ServerErrorResponses.getStreamError(StreamErrorCondition.UNSUPPORTED_VERSION, xmlLang,
                errorMessage + versionAttributeValue, null);
    }

}
