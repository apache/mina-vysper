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
package org.apache.vysper.xmpp.extension.xep0124;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the session state
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshBackedSessionContext extends AbstractSessionContext implements
        StanzaWriter {

    private final Logger logger = LoggerFactory
            .getLogger(BoshBackedSessionContext.class);

    private final BoshDecoder boshDecoder;

    private HttpServletRequest httpRequest;

    private HttpServletResponse httpRespone;

    /**
     * Creates a new context for a session
     * @param serverRuntimeContext
     * @param boshHandler
     */
    public BoshBackedSessionContext(ServerRuntimeContext serverRuntimeContext,
            BoshHandler boshHandler) {
        super(serverRuntimeContext, new SessionStateHolder());
        sessionStateHolder.setState(SessionState.INITIATED);
        boshDecoder = new BoshDecoder(boshHandler, this);
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void setIsReopeningXMLStream() {
    }

    public void write(Stanza stanza) {
        //        minaSession.write(new StanzaWriteInfo(stanza, !openingStanzaWritten));
    }

    public void close() {
        logger.info("session will be closed now");
    }

    public void switchToTLS() {
        // BOSH cannot switch dynamically,
        // SSL can be enabled/disabled in BoshEndpoint#setSSLEnabled()
    }

    /**
     * Updates the context with the session's {@link HttpServletRequest} and {@link HttpServletResponse}
     * <p>
     * The HTTP context is updated every time a new HTTP request is received.
     * @param req
     * @param resp
     */
    public void setHttpContext(HttpServletRequest req, HttpServletResponse resp) {
        httpRequest = req;
        httpRespone = resp;
    }

    /**
     * Getter for the HTTP request
     * @return
     */
    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Getter for the HTTP response
     * @return
     */
    public HttpServletResponse getHttpResponse() {
        return httpRespone;
    }

    /**
     * Getter for the decoder
     * @return
     */
    public BoshDecoder getDecoder() {
        return boshDecoder;
    }
}
