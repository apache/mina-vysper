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
package org.apache.vysper.xml.sax;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.decoder.XMPPContentHandler;
import org.apache.vysper.xml.decoder.XMPPContentHandler.StanzaListener;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.impl.DefaultAsyncXMLReader;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMPPContentHandlerTestCase extends TestCase {

	private static class TestListener implements StanzaListener {
		public List<XMLElement> elements = new ArrayList<XMLElement>();
		
		public void stanza(XMLElement element) {
			elements.add(element);
		}
	}
	
	public void test() throws Exception {
		AsyncXMLReader reader = new DefaultAsyncXMLReader();
		XMPPContentHandler handler = new XMPPContentHandler();
		TestListener listener = new TestListener();
		handler.setListener(listener);
		
		reader.setContentHandler(handler);
	
		parse(reader, "<stanza:stanza>");
		parse(reader, "<message></message>");
		parse(reader, "<iq>");
		parse(reader, "</iq>");
		parse(reader, "</stanza:stanza>");
		
		Iterator<XMLElement> actual = listener.elements.iterator();
		assertEquals("stanza", actual.next().getName());
		assertEquals("message", actual.next().getName());
		assertEquals("iq", actual.next().getName());
	}
	
	private void parse(AsyncXMLReader reader, String xml) throws Exception {
		reader.parse(ByteBuffer.wrap(xml.getBytes("UTF-8")), CharsetUtil.UTF8_DECODER);	
	}

}