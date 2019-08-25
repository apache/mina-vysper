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
package org.apache.vysper.stanzasession;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;

/**
 * a session running in the server VM based on using Vysper's built-in {@link org.apache.vysper.xmpp.stanza.Stanza}
 * object. this is an unconvential use, it does not rely on a network connection.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaSessionContext extends AbstractSessionContext implements StanzaWriter {

    protected Queue<Stanza> stanzaQueue = new LinkedList<Stanza>();
    
    private final StanzaProcessor stanzaProcessor;

    public StanzaSessionContext(ServerRuntimeContext serverRuntimeContext, StanzaProcessor stanzaProcessor, SessionStateHolder sessionStateHolder) {
        super(serverRuntimeContext, stanzaProcessor, sessionStateHolder);
        this.stanzaProcessor = stanzaProcessor;
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void sendStanzaToServer(Stanza stanza) {
        stanzaProcessor.processStanza(getServerRuntimeContext(), this, stanza,
                sessionStateHolder);
    }

    public void switchToTLS(boolean delayed, boolean clientTls) {
        if (sessionStateHolder.getState() == SessionState.ENCRYPTION_STARTED)
            sessionStateHolder.setState(SessionState.ENCRYPTED);
    }

    public void setIsReopeningXMLStream() {
        boolean isReopeningXMLStream = true; // currently not in use.
    }

    public void write(Stanza stanza) {
        stanzaQueue.add(stanza);
    }

    public void close() {
        return;
    }

    public Stanza getNextStanza() {
        return stanzaQueue.poll();
    }
}
