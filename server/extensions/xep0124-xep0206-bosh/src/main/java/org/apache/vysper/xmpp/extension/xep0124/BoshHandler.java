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

import java.io.IOException;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * Processes the BOSH requests from the clients and then responds back
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshHandler {

    /**
     * Handles the BOSH stanzas received from the decoder
     * @param sessionContext
     * @param stanza
     */
    public void processStanza(BoshBackedSessionContext sessionContext,
            Stanza stanza) {
        
        // TODO
        HttpServletResponse resp = sessionContext.getHttpResponse();
        String sid = Long.toString(new Random().nextLong(), 16);
        resp.setContentType("text/xml; charset=UTF-8");
        try {
            resp.getWriter().print("<body xmlns='http://jabber.org/protocol/httpbind' wait='60' inactivity='60' polling='5' requests='2' hold='1' maxpause='120' sid='");
        resp.getWriter().print(sid);
        resp.getWriter().print("' ver='1.6' from='vysper.org'/>");
        resp.flushBuffer();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
