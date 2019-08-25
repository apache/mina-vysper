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

import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.vysper.mina.codec.StanzaWriteInfo;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * connects MINA 2 frontend to the vysper backend
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MinaBackedSessionContext extends AbstractSessionContext implements StanzaWriter, IoFutureListener {

    final Logger logger = LoggerFactory.getLogger(MinaBackedSessionContext.class);

    private IoSession minaSession;

    private boolean openingStanzaWritten = false;

    private boolean switchToTLS = false;
    private boolean clientTLS = false;

    protected CloseFuture closeFuture;

    public MinaBackedSessionContext(ServerRuntimeContext serverRuntimeContext, StanzaProcessor stanzaProcessor, SessionStateHolder sessionStateHolder,
                                    IoSession minaSession) {
        super(serverRuntimeContext, stanzaProcessor, sessionStateHolder);
        this.minaSession = minaSession;
        closeFuture = minaSession.getCloseFuture();
        closeFuture.addListener(this);
        sessionStateHolder.setState(SessionState.INITIATED); // connection established
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void switchToTLS(boolean delayed, boolean clientTls) {
        this.clientTLS = clientTls;

        if(delayed) {
            switchToTLS = true;
        } else {
            addSslFilter();
        }
    }

    public void setIsReopeningXMLStream() {
        openingStanzaWritten = false;
    }
    
    private void addSslFilter() {
        
        minaSession.suspendRead();
        minaSession.suspendWrite();
        SslFilter filter = new SslFilter(getServerRuntimeContext().getSslContext());
        filter.setUseClientMode(clientTLS);
        minaSession.getFilterChain().addFirst("sslFilter", filter);
        if(!clientTLS) {
            minaSession.setAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE, Boolean.TRUE);
        }
        minaSession.setAttribute(SslFilter.USE_NOTIFICATION, Boolean.TRUE);
        minaSession.resumeWrite();
        minaSession.resumeRead();
        
    }

    public void write(Stanza stanza) {
        if (switchToTLS) {
            addSslFilter();
            switchToTLS = false;
        }

        minaSession.write(new StanzaWriteInfo(stanza, !openingStanzaWritten));
        openingStanzaWritten = true;
    }

    public void close() {
        logger.info("session will be closed now");
        closeFuture.setClosed();
        try {
            // allow some time to flush before closibng
            if(!minaSession.close(false).await(5000, TimeUnit.MILLISECONDS)) {
                // no really close if necessary
                minaSession.close(true);
            }
        } catch (InterruptedException e) {
            // ignore
        }
        logger.info("session closed");
    }

    public void operationComplete(IoFuture ioFuture) {
        // close future notification
        logger.info("close future called");
    }
}
