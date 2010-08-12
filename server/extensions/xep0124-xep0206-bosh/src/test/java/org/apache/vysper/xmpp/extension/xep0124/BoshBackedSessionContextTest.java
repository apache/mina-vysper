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
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BoshBackedSessionContextTest {

    private IMocksControl mocksControl;

    private BoshHandler boshHandler;

    private ServerRuntimeContext serverRuntimeContext;

    @Before
    public void setUp() throws Exception {
        mocksControl = createControl();
        boshHandler = mocksControl.createMock(BoshHandler.class);
        serverRuntimeContext = mocksControl.createMock(ServerRuntimeContext.class);
        expect(serverRuntimeContext.getNextSessionId()).andReturn("123");
        expect(serverRuntimeContext.getServerEnitity()).andReturn(new EntityImpl(null, "vysper.org", null));
        expect(serverRuntimeContext.getDefaultXMLLang()).andReturn("en");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testWrite0() {
        Continuation continuation = mocksControl.createMock(Continuation.class);
        HttpServletRequest httpServletRequest = mocksControl.createMock(HttpServletRequest.class);
        expect(httpServletRequest.getAttribute(Continuation.ATTRIBUTE)).andReturn(continuation);
        expectLastCall().atLeastOnce();
        continuation.setTimeout(anyLong());
        continuation.setAttribute(eq("request"), EasyMock.<BoshRequest> notNull());
        continuation.suspend();
        continuation.resume();
        continuation.addContinuationListener(EasyMock.<ContinuationListener> anyObject());
        Capture<BoshResponse> captured = new Capture<BoshResponse>();
        continuation.setAttribute(eq("response"), EasyMock.<BoshResponse> capture(captured));
        mocksControl.replay();

        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext);
        Stanza body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH).build();
        boshBackedSessionContext.insertRequest(new BoshRequest(httpServletRequest, body, 1L));
        boshBackedSessionContext.write0(body);
        mocksControl.verify();
        
        BoshResponse boshResponse = captured.getValue();
        assertEquals(BoshServlet.XML_CONTENT_TYPE, boshResponse.getContentType());
        assertEquals(new Renderer(body).getComplete(), new String(boshResponse.getContent()));
    }

    @Test
    public void testSetBoshVersion1() {
        mocksControl.replay();
        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext);
        boshBackedSessionContext.setBoshVersion("1.8");
        assertEquals("1.8", boshBackedSessionContext.getBoshVersion());
        mocksControl.verify();
    }

    @Test
    public void testSetBoshVersion2() {
        mocksControl.replay();
        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext);
        boshBackedSessionContext.setBoshVersion("2.0");
        assertEquals("1.9", boshBackedSessionContext.getBoshVersion());
        mocksControl.verify();
    }

    @Test
    public void testRequestExpired() {
        Stanza body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH).build();

        // addRequest
        HttpServletRequest httpServletRequest = mocksControl.createMock(HttpServletRequest.class);
        Continuation continuation = mocksControl.createMock(Continuation.class);
        expect(httpServletRequest.getAttribute(Continuation.ATTRIBUTE)).andReturn(continuation);
        expectLastCall().atLeastOnce();
        continuation.setTimeout(anyLong());
        continuation.suspend();
        continuation.setAttribute(eq("request"), EasyMock.<BoshRequest> notNull());

        Capture<ContinuationListener> listenerCaptured = new Capture<ContinuationListener>();
        continuation.addContinuationListener(EasyMock.<ContinuationListener> capture(listenerCaptured));
        
        BoshRequest br = new BoshRequest(httpServletRequest, body, 1L);

        // requestExpired
        expect(continuation.getAttribute("request")).andReturn(br);
        Capture<BoshResponse> responseCaptured = new Capture<BoshResponse>();
        continuation.setAttribute(eq("response"), EasyMock.<BoshResponse> capture(responseCaptured));

        expect(boshHandler.getEmptyResponse()).andReturn(body);
        expectLastCall().atLeastOnce();

        // write0
        continuation.resume();

        mocksControl.replay();
        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext);
        
        boshBackedSessionContext.insertRequest(br);
        listenerCaptured.getValue().onTimeout(continuation);
        mocksControl.verify();

        assertEquals(new Renderer(body).getComplete(), new String(responseCaptured.getValue().getContent()));
        assertEquals(BoshServlet.XML_CONTENT_TYPE, responseCaptured.getValue().getContentType());
    }

    @Test
    public void testAddRequest() {
        // addRequest
        HttpServletRequest httpServletRequest1 = mocksControl.createMock(HttpServletRequest.class);
        HttpServletRequest httpServletRequest2 = mocksControl.createMock(HttpServletRequest.class);
        Continuation continuation1 = mocksControl.createMock(Continuation.class);
        Continuation continuation2 = mocksControl.createMock(Continuation.class);
        expect(httpServletRequest1.getAttribute(Continuation.ATTRIBUTE)).andReturn(continuation1);
        expectLastCall().atLeastOnce();
        expect(httpServletRequest2.getAttribute(Continuation.ATTRIBUTE)).andReturn(continuation2);
        expectLastCall().atLeastOnce();
        continuation1.setTimeout(anyLong());
        continuation1.suspend();
        Capture<BoshRequest> br1 = new Capture<BoshRequest>();
        continuation1.setAttribute(eq("request"), EasyMock.<BoshRequest> capture(br1));
        continuation2.setTimeout(anyLong());
        continuation2.suspend();
        Capture<BoshRequest> br2 = new Capture<BoshRequest>();
        continuation2.setAttribute(eq("request"), EasyMock.<BoshRequest> capture(br2));
        continuation1.addContinuationListener(EasyMock.<ContinuationListener> anyObject());
        continuation2.addContinuationListener(EasyMock.<ContinuationListener> anyObject());
        
        Stanza body = new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH).build();
        expect(boshHandler.addAck(eq(body), EasyMock.anyLong())).andReturn(body);

        // write0
        Capture<BoshResponse> captured = new Capture<BoshResponse>();
        continuation1.setAttribute(eq("response"), EasyMock.<BoshResponse> capture(captured));
        continuation1.resume();

        mocksControl.replay();
        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext);

        boshBackedSessionContext.setHold(2);
        // consecutive writes with RID 1 and 2
        boshBackedSessionContext.insertRequest(new BoshRequest(httpServletRequest1, body, 1L));
        boshBackedSessionContext.insertRequest(new BoshRequest(httpServletRequest2, body, 2L));
        boshBackedSessionContext.write0(body);
        mocksControl.verify();
        
        assertEquals(httpServletRequest1, br1.getValue().getHttpServletRequest());
        assertEquals(httpServletRequest2, br2.getValue().getHttpServletRequest());

        assertEquals(new Renderer(body).getComplete(), new String(captured.getValue().getContent()));
        assertEquals(BoshServlet.XML_CONTENT_TYPE, captured.getValue().getContentType());
    }

    @Test
    public void testAddRequestWithDelayedResponses() {
        HttpServletRequest httpServletRequest = mocksControl.createMock(HttpServletRequest.class);
        Continuation continuation = mocksControl.createMock(Continuation.class);
        expect(httpServletRequest.getAttribute(Continuation.ATTRIBUTE)).andReturn(continuation);
        expectLastCall().atLeastOnce();
        continuation.setTimeout(anyLong());
        continuation.suspend();
        continuation.setAttribute(eq("request"), EasyMock.<BoshRequest> notNull());

        continuation.addContinuationListener(EasyMock.<ContinuationListener> anyObject());

        Stanza body1 = mocksControl.createMock(Stanza.class);
        Stanza body2 = mocksControl.createMock(Stanza.class);
        expect(boshHandler.mergeResponses(EasyMock.<Stanza> anyObject(), EasyMock.<Stanza> anyObject())).andReturn(
                new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH).build());
        expectLastCall().times(2);

        continuation.setAttribute(eq("response"), EasyMock.<BoshResponse> anyObject());
        continuation.resume();

        mocksControl.replay();

        BoshBackedSessionContext boshBackedSessionContext = new BoshBackedSessionContext(boshHandler, serverRuntimeContext);
        boshBackedSessionContext.write0(body1);
        boshBackedSessionContext.write0(body2);
        boshBackedSessionContext.insertRequest(new BoshRequest(httpServletRequest, body1, 1L));
        mocksControl.verify();
    }

}