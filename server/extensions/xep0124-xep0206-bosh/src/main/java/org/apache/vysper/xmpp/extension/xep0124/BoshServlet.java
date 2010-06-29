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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Receives BOSH requests from HTTP clients.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshServlet extends HttpServlet {

    public static final String TXT_CONTENT_TYPE = "text/plain";

    public static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    public static final String XML_CONTENT_TYPE = "text/xml; charset=UTF-8";

    private static final long serialVersionUID = 1979722775762481476L;

    private static final String FLASH_CROSS_DOMAIN_POLICY_URI = "/crossdomain.xml";

    private static final String INFO_GET = "This is an XMPP BOSH connection manager, you need to use a compatible BOSH client to use its services!";

    private static final String SERVER_IDENTIFICATION = "Vysper/0.5";

    private final Logger logger = LoggerFactory.getLogger(BoshServlet.class);

    private final BoshHandler boshHandler = new BoshHandler();

    private byte[] flashCrossDomainPolicy;

    private String accessControlAllowOrigin = "*";

    private String accessControlMaxAge = "86400"; // one day in seconds

    private String accessControlAllowMethods = "GET, POST, OPTIONS";

    /**
     * Setter for the {@link ServerRuntimeContext}
     * @param serverRuntimeContext
     */
    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        boshHandler.setServerRuntimeContext(serverRuntimeContext);
    }

    /**
     * Configures the Flash cross-domain policy
     * @param policyPath
     * @throws IOException 
     */
    public void setFlashCrossDomainPolicy(String policyPath) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(policyPath));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (;;) {
            int i = bis.read(buf);
            if (i == -1) {
                break;
            }
            baos.write(buf, 0, i);
        }
        bis.close();
        flashCrossDomainPolicy = baos.toByteArray();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addDateHeader("Date", System.currentTimeMillis());
        resp.addHeader("Server", SERVER_IDENTIFICATION);
        if (FLASH_CROSS_DOMAIN_POLICY_URI.equals(req.getRequestURI())) {
            resp.setContentType(XML_CONTENT_TYPE);
            resp.setContentLength(flashCrossDomainPolicy.length);
            resp.getOutputStream().write(flashCrossDomainPolicy);
        } else {
            resp.setContentType(HTML_CONTENT_TYPE);
            resp.setContentLength(INFO_GET.length());
            resp.getWriter().println(INFO_GET);
        }
        resp.flushBuffer();
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // used for preflighted requests
        resp.addDateHeader("Date", System.currentTimeMillis());
        resp.addHeader("Server", SERVER_IDENTIFICATION);
        resp.setContentType(TXT_CONTENT_TYPE);
        resp.setContentLength(0);
        resp.addHeader("Access-Control-Allow-Origin", accessControlAllowOrigin);
        resp.addHeader("Access-Control-Allow-Methods", accessControlAllowMethods);
        resp.addHeader("Access-Control-Max-Age", accessControlMaxAge);
        resp.flushBuffer();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BoshResponse boshResponse = (BoshResponse) req.getAttribute("response");
        if (boshResponse != null) {
            // if continuation is resumed or expired
            writeResponse(resp, boshResponse);
            return;
        }

        BoshDecoder decoder = new BoshDecoder(boshHandler, req);
        try {
            decoder.decode();
        } catch (SAXException e) {
            logger.error("Exception thrown while decoding XML", e);
        }
    }

    private void writeResponse(HttpServletResponse resp, BoshResponse respData) throws IOException {
        resp.addDateHeader("Date", System.currentTimeMillis());
        resp.addHeader("Server", SERVER_IDENTIFICATION);
        resp.setContentType(respData.getContentType());
        resp.setContentLength(respData.getContent().length);
        resp.addHeader("Access-control-allow-origin", accessControlAllowOrigin);
        resp.getOutputStream().write(respData.getContent());
        resp.flushBuffer();
    }

}
