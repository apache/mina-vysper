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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles BOSH requests from HTTP clients.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshServlet extends HttpServlet {

    private static final long serialVersionUID = 1979722775762481476L;

    private static final String FLASH_CROSS_DOMAIN_POLICY_URI = "/crossdomain.xml";

    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private static final String XML_CONTENT_TYPE = "text/xml; charset=UTF-8";

    private static final String INFO_GET = "This is an XMPP BOSH connection manager, you need to use a compatible BOSH client to use its services!";

    private final Logger logger = LoggerFactory.getLogger(BoshServlet.class);

    private ServerRuntimeContext serverRuntimeContext;

    private ByteArrayOutputStream flashCrossDomainPolicy;

    /**
     * Setter for the {@link ServerRuntimeContext}
     * @param serverRuntimeContext
     */
    public void setServerRuntimeContext(
            ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /**
     * Configures the Flash cross-domain policy
     * @param policyPath
     * @throws IOException 
     */
    public void setFlashCrossDomainPolicy(String policyPath) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                policyPath));
        flashCrossDomainPolicy = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (;;) {
            int i = bis.read(buf);
            if (i == -1) {
                break;
            }
            flashCrossDomainPolicy.write(buf, 0, i);
        }
        bis.close();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (FLASH_CROSS_DOMAIN_POLICY_URI.equals(req.getRequestURI())) {
            resp.setContentType(XML_CONTENT_TYPE);
            flashCrossDomainPolicy.writeTo(resp.getOutputStream());
        } else {
            resp.setContentType(HTML_CONTENT_TYPE);
            resp.getWriter().println(INFO_GET);
        }
        resp.flushBuffer();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        BufferedReader reader = req.getReader();

        char[] buf = new char[1024];
        StringBuilder sb = new StringBuilder();

        for (;;) {
            int n = reader.read(buf);
            if (n == -1) {
                break;
            }
            sb.append(buf, 0, n);
        }

        String body = sb.toString();
        logger.debug("BOSH CM received : {}", body);

        // test if this is the first request (kind of a hack - should be parsing XML)
        if (body.indexOf("sid=") == -1) {
            // initial request
            String sid = Long.toString(new Random().nextLong(), 16);
            resp.setContentType(XML_CONTENT_TYPE);
            resp.getWriter()
                    .print("<body xmlns='http://jabber.org/protocol/httpbind' wait='60' inactivity='60' polling='5' requests='2' hold='1' maxpause='120' sid='");
            resp.getWriter().print(sid);
            resp.getWriter().print("' ver='1.6' from='vysper.org'/>");
            resp.flushBuffer();
            return;
        }

        // session exists
        // not handled yet, TODO
        resp.setContentType(XML_CONTENT_TYPE);
        resp.flushBuffer();
    }

}
