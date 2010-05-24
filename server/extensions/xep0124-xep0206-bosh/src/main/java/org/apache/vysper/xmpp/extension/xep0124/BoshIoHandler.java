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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Bosh stanzas
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshIoHandler extends AbstractHandler {

    final Logger logger = LoggerFactory.getLogger(BoshIoHandler.class);

    private ServerRuntimeContext serverRuntimeContext;

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }
    
    public void handle(String target, Request baseRequest, 
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
    	
    	if (request.getMethod().equals("GET") && request.getPathInfo().equals("/crossdomain.xml")) {
    		// request for the cross-domain policy from Flash (e.g. flXHR)
    		response.setContentType("text/xml");
    		response.setStatus(HttpServletResponse.SC_OK);
    		response.getWriter().println("<cross-domain-policy><site-control permitted-cross-domain-policies='all'/><allow-access-from domain='*'/><allow-http-request-headers-from domain='*' headers='*'/></cross-domain-policy>");
    		((Request)request).setHandled(true);
    		return;
    	}
    	
    	if (!request.getMethod().equals("POST")) {
    		// it should not reach here normally, but it can happen if the user explicitly makes a non-POST request (e.g. GET, DELETE, etc)
    		response.setContentType("text/html");
    		response.setStatus(HttpServletResponse.SC_OK);
    		response.getWriter().println("<html>This is an XMPP BOSH connection manager, you need to use a compatible BOSH client to use its services!</html>");
    		((Request)request).setHandled(true);
    		return;
    	}
    	
    	BufferedReader reader = request.getReader();
    	
    	char [] buf = new char[1024];
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
    		response.setContentType("text/xml; charset=utf-8");
    		response.setStatus(HttpServletResponse.SC_OK);
    		response.getWriter().print("<body xmlns='http://jabber.org/protocol/httpbind' wait='60' inactivity='60' polling='5' requests='2' hold='1' maxpause='120' sid='");
    		response.getWriter().print(sid);
    		response.getWriter().print("' ver='1.6' from='vysper.org'/>");
    		((Request)request).setHandled(true);
    		return;
    	}
    	
    	// session exists
    	// not handled yet, TODO
    	response.setContentType("text/xml; charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
    	((Request)request).setHandled(true);
    }

}
