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
import java.util.List;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.sax.TestHandler.CharacterEvent;
import org.apache.vysper.xml.sax.TestHandler.EndDocumentEvent;
import org.apache.vysper.xml.sax.TestHandler.EndElementEvent;
import org.apache.vysper.xml.sax.TestHandler.FatalErrorEvent;
import org.apache.vysper.xml.sax.TestHandler.StartDocumentEvent;
import org.apache.vysper.xml.sax.TestHandler.StartElementEvent;
import org.apache.vysper.xml.sax.TestHandler.TestEvent;
import org.apache.vysper.xml.sax.impl.Attribute;
import org.apache.vysper.xml.sax.impl.DefaultAsyncXMLReader;
import org.apache.vysper.xml.sax.impl.DefaultAttributes;
import org.xml.sax.Attributes;


/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractAsyncXMLReaderTestCase extends TestCase {

	private static final Attributes EMPTY_ATTRIBUTES = new DefaultAttributes();
	
	protected void assertStartElement(String expectedUri, String expectedLocalName, String expectedQName, 
			TestEvent actual) {
		assertStartElement(expectedUri, expectedLocalName, expectedQName, EMPTY_ATTRIBUTES, actual);
	}

	protected Attributes attributes(Attribute...attributes) {
		List<Attribute> list = new ArrayList<Attribute>();
		for(Attribute attribute : attributes) {
			list.add(attribute);
		}
		return new DefaultAttributes(list);
	}
	
	protected void assertStartElement(String expectedUri, String expectedLocalName, String expectedQName, Attributes expectedAttributes, 
			TestEvent actual) {
		if(!(actual instanceof StartElementEvent)) fail("Event must be StartElementEvent");
		StartElementEvent startElementEvent = (StartElementEvent) actual;
		assertEquals("URI", expectedUri, startElementEvent.getURI());
		assertEquals("local name", expectedLocalName, startElementEvent.getLocalName());
		assertEquals("qName", expectedQName, startElementEvent.getQName());
		assertEquals("Attributes", expectedAttributes, startElementEvent.getAtts());
	}

	protected void assertEndElement(String expectedUri, String expectedLocalName, String expectedQName, 
			TestEvent actual) {
		if(!(actual instanceof EndElementEvent)) fail("Event must be EndElementEvent");
		EndElementEvent endElementEvent = (EndElementEvent) actual;
		assertEquals("URI", expectedUri, endElementEvent.getURI());
		assertEquals("local name", expectedLocalName, endElementEvent.getLocalName());
		assertEquals("qName", expectedQName, endElementEvent.getQName());
	}

	protected void assertStartDocument(TestEvent actual) {
		if(!(actual instanceof StartDocumentEvent)) fail("Event must be StartDocumentEvent");
	}

	protected void assertEndDocument(TestEvent actual) {
		if(!(actual instanceof EndDocumentEvent)) fail("Event must be EndDocumentEvent");
	}

	protected void assertText(String expected, TestEvent actual) {
		if(!(actual instanceof CharacterEvent)) fail("Event must be CharacterEvent");
		
		assertEquals(expected, ((CharacterEvent)actual).getCharacters());
	}

	protected List<TestEvent> parse(String xml) throws Exception {
		TestHandler handler = new TestHandler();
		AsyncXMLReader reader = new DefaultAsyncXMLReader();
		reader.setContentHandler(handler);
		reader.setErrorHandler(handler);

		reader.parse(ByteBuffer.wrap(xml.getBytes("UTF-8")), CharsetUtil.UTF8_DECODER);
		
		return handler.getEvents();
	}

	protected void assertFatalError(TestEvent actual) {
		if(!(actual instanceof FatalErrorEvent)) fail("Event must be FatalErrorEvent but is "+ actual.getClass());
	}

}