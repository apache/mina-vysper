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
package org.apache.vysper.xml.fragment;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.NamespaceURIs;
import org.apache.vysper.xml.fragment.StackNamespaceResolver;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;

import junit.framework.TestCase;

/**
 */
public class StackNamespaceResolverTestCase extends TestCase {

	private StackNamespaceResolver resolver = new StackNamespaceResolver();

	public void testPushSingleElement() {
		XMLElement elm = new XMLElementBuilder("foo")
			.addNamespaceAttribute("pr1", "url1")
			.addNamespaceAttribute("pr2", "url2").build();
		
		resolver.push(elm);
		
		assertEquals("url1", resolver.resolveUri("pr1"));
		assertEquals("url2", resolver.resolveUri("pr2"));
		assertNull(resolver.resolveUri("pr3"));

		assertEquals("pr1", resolver.resolvePrefix("url1"));
		assertEquals("pr2", resolver.resolvePrefix("url2"));
	}

	public void testImplicitNamespace() {
		XMLElement elm = new XMLElement("url1", "foo", "pr1", (Attribute[])null, null);
		
		resolver.push(elm);
		
		assertEquals("url1", resolver.resolveUri("pr1"));
		assertEquals("pr1", resolver.resolvePrefix("url1"));
	}

	public void testDefaultImplicitNamespace() {
		XMLElement elm = new XMLElement("url1", "foo", null, (Attribute[])null, null);
		
		resolver.push(elm);
		
		assertEquals("url1", resolver.resolveUri(""));

		assertEquals("", resolver.resolvePrefix("url1"));
	}

	
	public void testPushSingleNamespacedElement() {
		XMLElement elm = new XMLElementBuilder("foo", "defaulturl")
			.addNamespaceAttribute("pr1", "url1")
			.addNamespaceAttribute("pr2", "url2").build();
		
		resolver.push(elm);
		
		assertEquals("defaulturl", resolver.resolveUri(""));
		assertEquals("url1", resolver.resolveUri("pr1"));
		assertEquals("url2", resolver.resolveUri("pr2"));

		assertEquals("", resolver.resolvePrefix("defaulturl"));
		assertEquals("pr1", resolver.resolvePrefix("url1"));
		assertEquals("pr2", resolver.resolvePrefix("url2"));
	}
	
	public void testSimpleInheritance() {
		XMLElement elm = new XMLElementBuilder("foo", "defaulturl").build();
		XMLElement innerElm = new XMLElementBuilder("inner", "innerdefaulturl").build(); 
		
		resolver.push(elm);
		
		assertEquals("defaulturl", resolver.resolveUri(""));
		
		resolver.push(innerElm);
		
		assertEquals("innerdefaulturl", resolver.resolveUri(""));
		
		resolver.pop();
		
		assertEquals("defaulturl", resolver.resolveUri(""));
	}

	public void testPrefixedInheritance() {
		XMLElement elm = new XMLElementBuilder("foo", "url1")
		.addNamespaceAttribute("pr1", "url1")
		.build();
		XMLElement innerElm = new XMLElementBuilder("inner", "url1").build(); 
		
		resolver.push(elm);
		
		assertEquals("url1", resolver.resolveUri("pr1"));
		assertEquals("pr1", resolver.resolvePrefix("url1"));
		assertNull(resolver.resolveUri(""));
		
		resolver.push(innerElm);
		
		assertEquals("url1", resolver.resolveUri("pr1"));
		assertEquals("pr1", resolver.resolvePrefix("url1"));
		assertNull(resolver.resolveUri(""));
		
		resolver.pop();
		
		assertEquals("url1", resolver.resolveUri("pr1"));
		assertEquals("pr1", resolver.resolvePrefix("url1"));
		assertNull(resolver.resolveUri(""));
	}


	public void testPushXmlNamespace() {
		assertEquals(NamespaceURIs.XML, resolver.resolveUri("xml"));
		assertEquals("xml", resolver.resolvePrefix(NamespaceURIs.XML));
	}
}
