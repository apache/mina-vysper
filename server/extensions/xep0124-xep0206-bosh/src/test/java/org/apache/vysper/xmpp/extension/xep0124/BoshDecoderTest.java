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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.easymock.EasyMock.createStrictControl;
import static org.easymock.EasyMock.expect;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.vysper.xmpp.stanza.Stanza;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class BoshDecoderTest {
    
    private IMocksControl mocksControl;
    
    private BoshHandler boshHandler;
    private HttpServletRequest request;
    private BoshDecoder boshDecoder;

    @Before
    public void setUp() throws Exception {
        mocksControl = createStrictControl();
        boshHandler = mocksControl.createMock(BoshHandler.class);
        request = mocksControl.createMock(HttpServletRequest.class);
        boshDecoder = new BoshDecoder(boshHandler, request);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testDecode() throws IOException, SAXException {
        ServletInputStream input = new ServletInputStreamMock("<body rid='3549788615' xmlns='http://jabber.org/protocol/httpbind' to='vysper.org' xml:lang='en' wait='60' hold='1' ver='1.6' xmpp:version='1.0' xmlns:xmpp='urn:xmpp:xbosh'/>");
        expect(request.getInputStream()).andReturn(input);
        Capture<Stanza> captured = new Capture<Stanza>();
        boshHandler.process(EasyMock.<HttpServletRequest>notNull(), EasyMock.<Stanza>capture(captured));
        mocksControl.replay();
        boshDecoder.decode();
        mocksControl.verify();
        Stanza stanza = captured.getValue();
        assertNotNull(stanza);
        assertEquals("body", stanza.getName());
        assertEquals("http://jabber.org/protocol/httpbind", stanza.getNamespaceURI());
        assertEquals("3549788615", stanza.getAttributeValue("rid"));
        assertEquals("vysper.org", stanza.getAttributeValue("to"));
        assertEquals("60", stanza.getAttributeValue("wait"));
        assertEquals("1", stanza.getAttributeValue("hold"));
        assertEquals("1.6", stanza.getAttributeValue("ver"));
        assertEquals("1.0", stanza.getAttributeValue("urn:xmpp:xbosh", "version"));
        assertEquals("en", stanza.getAttributeValue("http://www.w3.org/XML/1998/namespace", "lang"));
    }

}
