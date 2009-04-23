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
package org.apache.vysper.mina;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.TrafficMask;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoFuture;
import org.apache.mina.filter.SSLFilter;
import org.apache.vysper.mina.codec.StanzaWriteInfo;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * connects MINA frontend to the vysper backend
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class MinaBackedSessionContext extends AbstractSessionContext implements StanzaWriter, IoFutureListener {

    final Logger logger = LoggerFactory.getLogger(MinaBackedSessionContext.class);

    private IoSession minaSession;
    private boolean openingStanzaWritten = false;
    private boolean switchToTLS = false;
    protected CloseFuture closeFuture;

    public MinaBackedSessionContext(
            ServerRuntimeContext serverRuntimeContext,
            SessionStateHolder sessionStateHolder,
            IoSession minaSession) {
        super(serverRuntimeContext, sessionStateHolder);
        this.minaSession = minaSession;
        closeFuture = minaSession.getCloseFuture();
        closeFuture.addListener(this);
        sessionStateHolder.setState(SessionState.INITIATED); // connection established
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void switchToTLS() {
        switchToTLS = true;
    }

    public void setIsReopeningXMLStream() {
        openingStanzaWritten = false;
    }

    public void write(Stanza stanza) {

        if (switchToTLS) {
            minaSession.setTrafficMask(TrafficMask.WRITE);
            SSLFilter filter = new SSLFilter(getServerRuntimeContext().getSslContext());
            filter.setUseClientMode(false);
            minaSession.getFilterChain().addFirst("sslFilter", filter);
            minaSession.setAttribute(SSLFilter.DISABLE_ENCRYPTION_ONCE, Boolean.TRUE);
            minaSession.setAttribute(SSLFilter.USE_NOTIFICATION, Boolean.TRUE);
            minaSession.setTrafficMask(TrafficMask.ALL);
            switchToTLS = false;
        }

        minaSession.write(new StanzaWriteInfo(stanza, !openingStanzaWritten));
        openingStanzaWritten = true;
    }

    public void close() {
        logger.info("session will be closed now");
        closeFuture.setClosed();
        minaSession.close();
    }

    public void operationComplete(IoFuture ioFuture) {
        // close future notification
        logger.info("close future called");
    }
}
