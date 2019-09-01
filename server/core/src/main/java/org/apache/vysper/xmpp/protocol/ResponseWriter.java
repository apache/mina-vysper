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
package org.apache.vysper.xmpp.protocol;

import org.apache.vysper.xmpp.parser.ParsingErrorCondition;
import org.apache.vysper.xmpp.parser.ParsingException;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.StanzaReceivingSessionContext;
import org.apache.vysper.xmpp.server.XMPPVersion;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * writes protocol level errors
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ResponseWriter {

    public static void writeUnsupportedStanzaError(StanzaReceivingSessionContext sessionContext) {

        Stanza errorStanza = ServerErrorResponses.getStreamError(StreamErrorCondition.UNSUPPORTED_STANZA_TYPE,
                sessionContext.getXMLLang(), "service unavailable at this session state", null);
        Stanza streamOpener = new ServerResponses().getStreamOpenerForError(false, sessionContext.getServerJID(),
                XMPPVersion.VERSION_1_0, errorStanza);

        writeErrorAndClose(sessionContext, streamOpener);
    }

    public static void handleProtocolError(ProtocolException protocolException, StanzaReceivingSessionContext sessionContext,
                                           Stanza receivedStanza) {
        Stanza errorStanza = null;
        if (protocolException != null)
            errorStanza = protocolException.getErrorStanza();

        if (errorStanza == null) {
            errorStanza = ServerErrorResponses.getStreamError(StreamErrorCondition.BAD_FORMAT,
                    sessionContext.getXMLLang(), "could not process incoming stanza", receivedStanza);
        }
        writeErrorAndClose(sessionContext, errorStanza);
    }

    public void handleUnsupportedStanzaType(StanzaReceivingSessionContext sessionContext, Stanza receivedStanza) {
        Stanza errorStanza = ServerErrorResponses.getStreamError(StreamErrorCondition.UNSUPPORTED_STANZA_TYPE,
                sessionContext.getXMLLang(), "could not process incoming stanza", receivedStanza);
        writeErrorAndClose(sessionContext, errorStanza);
    }

    public void handleNotAuthorized(StanzaReceivingSessionContext sessionContext, Stanza receivedStanza) {
        Stanza errorStanza = ServerErrorResponses.getStreamError(StreamErrorCondition.NOT_AUTHORIZED,
                sessionContext.getXMLLang(), "could not process incoming stanza", receivedStanza);
        writeErrorAndClose(sessionContext, errorStanza);
    }

    public void handleWrongFromJID(StanzaReceivingSessionContext sessionContext, Stanza receivedStanza) {
        XMPPCoreStanza receivedCoreStanza = XMPPCoreStanza.getWrapper(receivedStanza);
        if (receivedCoreStanza == null) {
            handleNotAuthorized(sessionContext, receivedStanza);
            return;
        }

        Stanza errorStanza = ServerErrorResponses.getStanzaError(StanzaErrorCondition.UNKNOWN_SENDER,
                receivedCoreStanza, StanzaErrorType.MODIFY, "from attribute does not match authorized entity", null,
                null);
        sessionContext.getResponseWriter().write(errorStanza);
    }

    public static void writeErrorAndClose(StanzaReceivingSessionContext sessionContext, Stanza errorStanza) {
        sessionContext.getResponseWriter().write(errorStanza);
        sessionContext.endSession(SessionContext.SessionTerminationCause.STREAM_ERROR);
    }

}
