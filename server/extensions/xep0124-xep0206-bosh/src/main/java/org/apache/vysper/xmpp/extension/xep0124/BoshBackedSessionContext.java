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

    private final static Logger LOGGER = LoggerFactory
            .getLogger(BoshBackedSessionContext.class);

    private final int inactivity = 60;

    private final int polling = 15;

    private final int requests = 2;

    private String ver = "1.9";

    private String contentType = ContentType.XML_CONTENT_TYPE;

    private int wait = 60;

    private int hold = 1;

    /**
     * Creates a new context for a session
     * @param serverRuntimeContext
     * @param boshHandler
     */
    public BoshBackedSessionContext(ServerRuntimeContext serverRuntimeContext) {
        super(serverRuntimeContext, new SessionStateHolder());
        sessionStateHolder.setState(SessionState.INITIATED);
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void setIsReopeningXMLStream() {
    }

    public void write(Stanza stanza) {
    }

    public void close() {
        LOGGER.info("session will be closed now");
    }

    public void switchToTLS() {
        // BOSH cannot switch dynamically,
        // SSL can be enabled/disabled in BoshEndpoint#setSSLEnabled()
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setWait(int wait) {
        this.wait = Math.min(wait, this.wait);
    }

    public int getWait() {
        return wait;
    }

    public void setHold(int hold) {
        this.hold = Math.min(hold, this.hold);
    }

    public int getHold() {
        return hold;
    }

    public void setVer(String ver) {
        String[] serverVer = this.ver.split("\\.");
        int serverMajor = Integer.parseInt(serverVer[0]);
        int serverMinor = Integer.parseInt(serverVer[1]);
        String[] clientVer = ver.split("\\.");
        
        if (clientVer.length == 2) {
            int clientMajor = Integer.parseInt(clientVer[0]);
            int clientMinor = Integer.parseInt(clientVer[1]);

            if (clientMajor < serverMajor
                    || (clientMajor == serverMajor && clientMinor < serverMinor)) {
                this.ver = ver;
            }
        }
    }

    public String getVer() {
        return ver;
    }

    public int getInactivity() {
        return inactivity;
    }

    public int getPolling() {
        return polling;
    }

    public int getRequests() {
        return requests;
    }

}
