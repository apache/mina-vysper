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

package org.apache.vysper.xmpp.server.s2s;

import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class TlsProceedHandler implements StanzaHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(TlsProceedHandler.class);
    
    public String getName() {
        return "proceed";
    }

    public boolean verify(Stanza stanza) {
        if (stanza == null) return false;
        if (!getName().equals(stanza.getName())) return false;
        String namespaceURI = stanza.getNamespaceURI();
        return namespaceURI.equals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
    }

    public boolean isSessionRequired() {
        return true;
    }

    public void execute(Stanza stanza, ServerRuntimeContext serverRuntimeContext,
						boolean isOutboundStanza, SessionContext sessionContext, SessionStateHolder sessionStateHolder, StanzaBroker stanzaBroker) {
        sessionStateHolder.setState(SessionState.ENCRYPTION_STARTED);
        
        LOG.debug("XMPP server connector switching to TLS");
        sessionContext.switchToTLS(false, true);
    }
}
