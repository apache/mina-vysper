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
package org.apache.vysper.xml.sax.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.decoder.XMLElementListener;
import org.apache.vysper.xml.decoder.XMPPContentHandler;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMPPContentHandlerTestCase extends TestCase {

    private static class TestListener implements XMLElementListener {
        public List<XMLElement> elements = new ArrayList<XMLElement>();
        private boolean closed = false;

        public void element(XMLElement element) {
            elements.add(element);
        }
        
        public void close() {
        	closed = true;
        }
        
        public boolean isClosed() {
        	return closed;
        }
    }

    public void test() throws Exception {
        NonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        XMPPContentHandler handler = new XMPPContentHandler();
        TestListener listener = new TestListener();
        handler.setListener(listener);

        reader.setContentHandler(handler);

        parse(reader, "<stanza:stanza xmlns:stanza='http://etherx.jabber.org/streams'>");
        parse(reader, "<message></message>");
        parse(reader, "<iq>");
        parse(reader, "</iq>");
        parse(reader, "</stanza:stanza>");

        Iterator<XMLElement> actual = listener.elements.iterator();
        assertEquals("stanza", actual.next().getName());
        assertEquals("message", actual.next().getName());
        assertEquals("iq", actual.next().getName());
        assertEquals(true, listener.isClosed());
    }

    private void parse(NonBlockingXMLReader reader, String xml) throws Exception {
        reader.parse(IoBuffer.wrap(xml.getBytes("UTF-8")), CharsetUtil.getDecoder());
    }

}