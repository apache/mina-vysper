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

import org.apache.mina.core.session.IoSession;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerTimeoutException;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;

/**
 * Connector for outgoing connections to other XMPP servers. 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface XMPPServerConnector extends StanzaWriter {

    /**
     * Connect and authenticate the XMPP server connector
     */
    void start() throws RemoteServerNotFoundException, RemoteServerTimeoutException;
    
    /**
     * Is this XMPP server connector closed?
     * @return true if the connector is closed
     */
    boolean isClosed();

    
    /**
     * Write a {@link Stanza} to another XMPP server
     * @param stanza The {@link Stanza} to write
     */
    void write(Stanza stanza);

    void handleReceivedStanza(Stanza stanza);

    void handleSessionSecured();

    void handleSessionOpened(IoSession session);
}