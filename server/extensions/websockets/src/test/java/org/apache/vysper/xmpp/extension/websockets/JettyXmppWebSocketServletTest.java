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
package org.apache.vysper.xmpp.extension.websockets;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.eclipse.jetty.websocket.WebSocket;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class JettyXmppWebSocketServletTest {

    private ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);
    private StanzaProcessor stanzaProcessor = Mockito.mock(StanzaProcessor.class);

    @Test
    public void doWebSocketConnectWithDefaultCstr() throws ServletException {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(JettyXmppWebSocketServlet.SERVER_RUNTIME_CONTEXT_ATTRIBUTE)).thenReturn(serverRuntimeContext);
        Mockito.when(servletContext.getAttribute(StanzaProcessor.class.getCanonicalName())).thenReturn(stanzaProcessor);

        ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
        Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);

        JettyXmppWebSocketServlet servlet = new JettyXmppWebSocketServlet();
        servlet.init(servletConfig);

        WebSocket webSocket = servlet.doWebSocketConnect(null, "xmpp");
        Assert.assertTrue(webSocket instanceof JettyXmppWebSocket);
    }

    @Test
    public void doWebSocketConnectWithDirectCstr() throws ServletException {
      JettyXmppWebSocketServlet servlet = new JettyXmppWebSocketServlet(serverRuntimeContext, stanzaProcessor);

        WebSocket webSocket = servlet.doWebSocketConnect(null, "xmpp");
        Assert.assertTrue(webSocket instanceof JettyXmppWebSocket);
    }

    @Test
    public void doWebSocketConnectWithInvalidSubprotocl() throws ServletException {
      JettyXmppWebSocketServlet servlet = new JettyXmppWebSocketServlet(serverRuntimeContext, stanzaProcessor);

        WebSocket webSocket = servlet.doWebSocketConnect(null, "dummy");
        Assert.assertNull(webSocket);
    }

    @Test(expected=RuntimeException.class)
    public void doWebSocketConnectMissingAttribute() throws ServletException {
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
        Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);

        JettyXmppWebSocketServlet servlet = new JettyXmppWebSocketServlet();
        servlet.init(servletConfig);
    }

}
