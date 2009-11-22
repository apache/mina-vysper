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
package org.apache.vysper.xmpp.xmlfragment;

import junit.framework.TestCase;

public class RendererTestCase extends TestCase {

	public void testRenderAttribute() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "value1")}, null);
		assertEquals("<foo attr1=\"value1\"></foo>", new Renderer(elm).getComplete());
	}

	// & must be escaped
	public void testRenderAttributeWithAmpersand() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val&ue1")}, null);
		assertEquals("<foo attr1=\"val&amp;ue1\"></foo>", new Renderer(elm).getComplete());
	}

	public void testRenderAttributeWithQuot() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val\"ue1")}, null);
		assertEquals("<foo attr1=\"val&quot;ue1\"></foo>", new Renderer(elm).getComplete());
	}

	public void testRenderAttributeWithApos() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val'ue1")}, null);
		assertEquals("<foo attr1=\"val'ue1\"></foo>", new Renderer(elm).getComplete());
	}

	// > is not required to be escaped, but we do so to make sure
	public void testRenderAttributeWithGt() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val>ue1")}, null);
		assertEquals("<foo attr1=\"val&gt;ue1\"></foo>", new Renderer(elm).getComplete());
	}

	// < must be escaped
	public void testRenderAttributeWithLt() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val<ue1")}, null);
		assertEquals("<foo attr1=\"val&lt;ue1\"></foo>", new Renderer(elm).getComplete());
	}

	public void testRenderNamespacedAttribute() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{
				new Attribute("http://example.com", "attr1", "value1"),
				new NamespaceAttribute("pr1", "http://example.com")
				}, null);
		assertEquals("<foo pr1:attr1=\"value1\" xmlns:pr1=\"http://example.com\"></foo>", new Renderer(elm).getComplete());
	}

	// make sure we render the xml namespace correctly, e.g for xml:lang
	public void testRenderXmlNamespacedAttribute() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{
				new Attribute(NamespaceURIs.XML, "lang", "sv")
				}, null);
		assertEquals("<foo xml:lang=\"sv\"></foo>", new Renderer(elm).getComplete());
	}
	
	public void testRenderUndeclaredNamespacedAttribute() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{
				new Attribute("http://example.com", "attr1", "value1")
				}, null);
		try {
			new Renderer(elm).getComplete();
			fail("Must throw IllegalStateException");
		} catch (IllegalStateException e) {
			// ok
		}
	}
	
	public void testRenderNonNamespaceElement() {
		XMLElement elm = new XMLElement(null, "foo", null, (Attribute[])null, null);
		assertEquals("<foo></foo>", new Renderer(elm).getComplete());
	}
	
	public void testRenderDefaultNamespaceElement() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{
				new NamespaceAttribute("http://example.com")
		}, null);
		assertEquals("<foo xmlns=\"http://example.com\"></foo>", new Renderer(elm).getComplete());
	}

	public void testRenderPrefixedNamespaceElement() {
		XMLElement elm = new XMLElement(null, "foo", "pr", new Attribute[]{
				new NamespaceAttribute("pr", "http://example.com")
		}, null);
		assertEquals("<pr:foo xmlns:pr=\"http://example.com\"></pr:foo>", new Renderer(elm).getComplete());
	}

	public void testRenderSimpleText() {
		XMLElement elm = new XMLElement(null, "foo", null, null, new XMLFragment[]{
				new XMLText("bar")
		});
		assertEquals("<foo>bar</foo>", new Renderer(elm).getComplete());
	}

	public void testRenderTextWithAmpersand() {
		XMLElement elm = new XMLElement(null, "foo", null, null, new XMLFragment[]{
				new XMLText("ba&r")
		});
		assertEquals("<foo>ba&amp;r</foo>", new Renderer(elm).getComplete());
	}

	public void testRenderTextWithGt() {
		XMLElement elm = new XMLElement(null, "foo", null, null, new XMLFragment[]{
				new XMLText("ba>r")
		});
		assertEquals("<foo>ba&gt;r</foo>", new Renderer(elm).getComplete());
	}

	public void testRenderTextWithLt() {
		XMLElement elm = new XMLElement(null, "foo", null, null, new XMLFragment[]{
				new XMLText("ba<r")
		});
		assertEquals("<foo>ba&lt;r</foo>", new Renderer(elm).getComplete());
	}

	
	// TODO test allowed Unicode characters ranged in element name attribute name, attributes values, text
}
