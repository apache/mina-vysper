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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BoshBackedSessionContextTest {

    private IMocksControl mocksControl;

    private BoshHandler boshHandler;

    private ServerRuntimeContext serverRuntimeContext;
    
    private InactivityChecker inactivityChecker;

    @Before
    public void setUp() throws Exception {
        mocksControl = createControl();
        boshHandler = mocksControl.createMock(BoshHandler.class);
        serverRuntimeContext = mocksControl.createMock(ServerRuntimeContext.class);
        expect(serverRuntimeContext.getNextSessionId()).andReturn("123");
        expect(serverRuntimeContext.getServerEnitity()).andReturn(new EntityImpl(null, "vysper.org", null));
        expect(serverRuntimeContext.getDefaultXMLLang()).andReturn("en");
        inactivityChecker = new InactivityChecker();
//        inactivityChecker.start();
    }

    @After
    public void tearDown() throws Exception {
//        inactivityChecker.interrupt();
    }

    @Test
    public void testWrite0() {
        HttpServletRequest httpServletRequest = mocksControl.createMock(HttpServletRequest.class);
        AsyncContext asyncContext = mocksControl.createMock(AsyncContext.class);
        expect(httpServletRequest.startAsync()).andReturn(asyncContext);
        expectLastCall().atLeastOnce();
        expect(httpServletRequest.getAsyncContext()).andReturn(asyncContext);
        expectLastCall().atLeastOnce();
        asyncContext.setTimeout(anyLong());
        asyncContext.dispatch();
        expectLastCall().atLeastOnce();
        httpServletRequest.setAttribute(eq("request"), EasyMock.<BoshRequest> notNull());
        asyncContext.addListener(EasyMock.<AsyncListener> anyObject());
        Capture<BoshResponse> captured = new Capture<BoshResponse>();
        httpServletRequest.setAttribute(eq("response"), EasyMock.<BoshResponse> capture(captured));
        mocksControl.replay();

        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext, inactivityChecker);
        Stanza body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH).build();
        boshBackedSessionContext.insertRequest(new BoshRequest(httpServletRequest, body, 1L));
        boshBackedSessionContext.writeBOSHResponse(body);
        mocksControl.verify();
        
        BoshResponse boshResponse = captured.getValue();
        assertEquals(BoshServlet.XML_CONTENT_TYPE, boshResponse.getContentType());
        assertEquals(new Renderer(body).getComplete(), new String(boshResponse.getContent()));
    }

    @Test
    public void testSetBoshVersion1() {
        mocksControl.replay();
        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext, inactivityChecker);
        boshBackedSessionContext.setBoshVersion("1.8");
        assertEquals("1.8", boshBackedSessionContext.getBoshVersion());
        mocksControl.verify();
    }

    @Test
    public void testSetBoshVersion2() {
        mocksControl.replay();
        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext, inactivityChecker);
        boshBackedSessionContext.setBoshVersion("2.0");
        assertEquals("1.9", boshBackedSessionContext.getBoshVersion());
        mocksControl.verify();
    }

    @Test
    public void testRequestExpired() throws IOException {
        Stanza emtpyStanza = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH).build();

        // addRequest
        HttpServletRequest httpServletRequest = mocksControl.createMock(HttpServletRequest.class);
        AsyncContext asyncContext = mocksControl.createMock(AsyncContext.class);
        expect(httpServletRequest.startAsync()).andReturn(asyncContext).atLeastOnce();
        expect(httpServletRequest.getAsyncContext()).andReturn(asyncContext).atLeastOnce();
        asyncContext.setTimeout(anyLong());
        httpServletRequest.setAttribute(eq("request"), EasyMock.<BoshRequest> notNull());

        expect(asyncContext.getRequest()).andReturn(httpServletRequest).atLeastOnce();
        asyncContext.dispatch();
        expectLastCall().atLeastOnce();

        Capture<AsyncListener> listenerCaptured = new Capture<AsyncListener>();
        asyncContext.addListener(EasyMock.<AsyncListener> capture(listenerCaptured));

        AsyncEvent asyncEvent = mocksControl.createMock(AsyncEvent.class);

        BoshRequest br = new BoshRequest(httpServletRequest, emtpyStanza, 1L);

        // requestExpired
        expect(httpServletRequest.getAttribute("request")).andReturn(br);
        Capture<BoshResponse> responseCaptured = new Capture<BoshResponse>();
        httpServletRequest.setAttribute(eq("response"), EasyMock.<BoshResponse> capture(responseCaptured));

        // write0
        mocksControl.replay();
        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext, inactivityChecker);
        
        boshBackedSessionContext.insertRequest(br);
        listenerCaptured.getValue().onTimeout(asyncEvent);
        mocksControl.verify();

        assertEquals(new Renderer(emtpyStanza).getComplete(), new String(responseCaptured.getValue().getContent()));
        assertEquals(BoshServlet.XML_CONTENT_TYPE, responseCaptured.getValue().getContentType());
    }

    @Test
    public void testAddRequest() {
        // addRequest
        HttpServletRequest httpServletRequest1 = mocksControl.createMock(HttpServletRequest.class);
        HttpServletRequest httpServletRequest2 = mocksControl.createMock(HttpServletRequest.class);
        AsyncContext asyncContext1 = mocksControl.createMock(AsyncContext.class);
        AsyncContext asyncContext2 = mocksControl.createMock(AsyncContext.class);

        expect(httpServletRequest1.startAsync()).andReturn(asyncContext1).atLeastOnce();
        expect(httpServletRequest1.getAsyncContext()).andReturn(asyncContext1).atLeastOnce();

        expect(httpServletRequest2.startAsync()).andReturn(asyncContext2).atLeastOnce();
        expect(httpServletRequest2.getAsyncContext()).andReturn(asyncContext2).anyTimes();

        asyncContext1.setTimeout(anyLong());
        Capture<BoshRequest> br1 = new Capture<BoshRequest>();
        httpServletRequest1.setAttribute(eq("request"), EasyMock.<BoshRequest> capture(br1));

        asyncContext2.setTimeout(anyLong());
        Capture<BoshRequest> br2 = new Capture<BoshRequest>();
        httpServletRequest2.setAttribute(eq("request"), EasyMock.<BoshRequest> capture(br2));

        asyncContext1.addListener(EasyMock.<AsyncListener> anyObject());
        asyncContext2.addListener(EasyMock.<AsyncListener> anyObject());

        asyncContext1.dispatch();
        expectLastCall().atLeastOnce();

        Stanza body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH).build();
        expect(boshHandler.addAttribute(eq(body), eq("ack"), Long.toString(EasyMock.anyLong()))).andReturn(body);

        // write0
        Capture<BoshResponse> captured = new Capture<BoshResponse>();
        httpServletRequest1.setAttribute(eq("response"), EasyMock.<BoshResponse> capture(captured));

        mocksControl.replay();
        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext, inactivityChecker);

        boshBackedSessionContext.setHold(2);
        // consecutive writes with RID 1 and 2
        boshBackedSessionContext.insertRequest(new BoshRequest(httpServletRequest1, body, 1L));
        boshBackedSessionContext.insertRequest(new BoshRequest(httpServletRequest2, body, 2L));
        boshBackedSessionContext.writeBOSHResponse(body);
        mocksControl.verify();

        assertEquals(httpServletRequest1, br1.getValue().getHttpServletRequest());
        assertEquals(httpServletRequest2, br2.getValue().getHttpServletRequest());

        assertEquals(new Renderer(body).getComplete(), new String(captured.getValue().getContent()));
        assertEquals(BoshServlet.XML_CONTENT_TYPE, captured.getValue().getContentType());
    }

    @Test
    public void testAddRequestWithDelayedResponses() {
        HttpServletRequest httpServletRequest = mocksControl.createMock(HttpServletRequest.class);
        AsyncContext asyncContext = mocksControl.createMock(AsyncContext.class);
        expect(httpServletRequest.startAsync()).andReturn(asyncContext).atLeastOnce();
        expect(httpServletRequest.getAsyncContext()).andReturn(asyncContext).atLeastOnce();
        asyncContext.setTimeout(anyLong());
        httpServletRequest.setAttribute(eq("request"), EasyMock.<BoshRequest> notNull());

        asyncContext.addListener(EasyMock.<AsyncListener> anyObject());

        asyncContext.dispatch();
        expectLastCall().atLeastOnce();

        Stanza body1 = mocksControl.createMock(Stanza.class);
        Stanza body2 = mocksControl.createMock(Stanza.class);
        Stanza body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH).build();
        expect(boshHandler.mergeResponses(EasyMock.<Stanza> anyObject(), EasyMock.<Stanza> anyObject()))
                .andReturn(body);
        expectLastCall().times(2);

        httpServletRequest.setAttribute(eq("response"), EasyMock.<BoshResponse> anyObject());

        mocksControl.replay();

        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler,
                serverRuntimeContext, inactivityChecker);
        boshBackedSessionContext.writeBOSHResponse(body1);
        boshBackedSessionContext.writeBOSHResponse(body2);
        boshBackedSessionContext.insertRequest(new BoshRequest(httpServletRequest, body, 1L));
        mocksControl.verify();
    }
}
