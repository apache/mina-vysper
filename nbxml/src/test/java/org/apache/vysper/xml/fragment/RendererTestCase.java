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
import org.apache.vysper.xml.fragment.NamespaceAttribute;
import org.apache.vysper.xml.fragment.NamespaceURIs;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLText;

import junit.framework.TestCase;

public class RendererTestCase extends TestCase {

	public void testRenderAttribute() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "value1")}, null);
		assertRendering("<foo attr1=\"value1\"></foo>", elm);
	}

	// & must be escaped
	public void testRenderAttributeWithAmpersand() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val&ue1")}, null);
		assertRendering("<foo attr1=\"val&amp;ue1\"></foo>", elm);
	}

	public void testRenderAttributeWithQuot() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val\"ue1")}, null);
		assertRendering("<foo attr1=\"val&quot;ue1\"></foo>", elm);
	}

	public void testRenderAttributeWithApos() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val'ue1")}, null);
		assertRendering("<foo attr1=\"val'ue1\"></foo>", elm);
	}

	// > is not required to be escaped, but we do so to make sure
	public void testRenderAttributeWithGt() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val>ue1")}, null);
		assertRendering("<foo attr1=\"val&gt;ue1\"></foo>", elm);
	}

	// < must be escaped
	public void testRenderAttributeWithLt() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{new Attribute("attr1", "val<ue1")}, null);
		assertRendering("<foo attr1=\"val&lt;ue1\"></foo>", elm);
	}

	public void testRenderNamespacedAttribute() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{
				new Attribute("http://example.com", "attr1", "value1"),
				new NamespaceAttribute("pr1", "http://example.com")
				}, null);
		assertRendering("<foo xmlns:pr1=\"http://example.com\" pr1:attr1=\"value1\"></foo>", elm);
	}

	// make sure we render the xml namespace correctly, e.g for xml:lang
	public void testRenderXmlNamespacedAttribute() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{
				new Attribute(NamespaceURIs.XML, "lang", "sv")
				}, null);
		assertRendering("<foo xml:lang=\"sv\"></foo>", elm);
	}
	
	public void testRenderUndeclaredNamespacedAttribute() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{
				new Attribute("http://example.com", "attr1", "value1")
				}, null);
		
		assertRendering("<foo xmlns:ns1=\"http://example.com\" ns1:attr1=\"value1\"></foo>", elm);
	}

//	public void testRenderEmptyElement() {
//		XMLElement elm = new XMLElement(null, "foo", null, (Attribute[])null, null);
//		assertRendering("<foo />", elm);
//	}
//
//	public void testRenderEmptyElementWithAttribute() {
//		XMLElement elm = new XMLElementBuilder("foo").addAttribute("attr", "value").getFinalElement();
//
//		assertRendering("<foo attr=\"value\" />", elm);
//	}
	
	public void testRenderNonNamespaceElement() {
		XMLElement elm = new XMLElement(null, "foo", null, (Attribute[])null, null);
		assertRendering("<foo></foo>", elm);
	}
	
	public void testRenderDefaultNamespaceElement() {
		XMLElement elm = new XMLElement(null, "foo", null, new Attribute[]{
				new NamespaceAttribute("http://example.com")
		}, null);
		assertRendering("<foo xmlns=\"http://example.com\"></foo>", elm);
	}

	public void testRenderPrefixedNamespaceWithDeclarationElement() {
		XMLElement elm = new XMLElement("http://example.com", "foo", "pr", new Attribute[]{
				new NamespaceAttribute("pr", "http://example.com")
		}, null);
		assertRendering("<pr:foo xmlns:pr=\"http://example.com\"></pr:foo>", elm);
	}

	public void testRenderPrefixedNamespaceElement() {
		XMLElement elm = new XMLElement("http://example.com", "foo", "pr", (Attribute[])null, null);
		assertRendering("<pr:foo xmlns:pr=\"http://example.com\"></pr:foo>", elm);
	}

	public void testRenderDeclaredNamespaceElement() {
		XMLElementBuilder builder = new XMLElementBuilder("foo", "http://example.com");
		builder.addNamespaceAttribute("pr", "http://example.com");
		assertRendering("<pr:foo xmlns:pr=\"http://example.com\"></pr:foo>", builder.build());
	}

	public void testRenderInnerNamespacedElement() {
		XMLElementBuilder builder = new XMLElementBuilder("foo", "http://example.com");
		builder.addNamespaceAttribute("pr", "http://other.com");
		builder.startInnerElement("bar", "http://other.com");
		assertRendering("<foo xmlns:pr=\"http://other.com\" xmlns=\"http://example.com\"><pr:bar></pr:bar></foo>", builder.build());
	}

	public void testRenderInnerInheritedDefaultNamespaceElement() {
		XMLElementBuilder builder = new XMLElementBuilder("foo", "http://example.com");
		builder.startInnerElement("bar", "http://example.com");
		assertRendering("<foo xmlns=\"http://example.com\"><bar></bar></foo>", builder.build());
	}

	public void testRenderInnerInheritedNamespaceElement() {
		XMLElementBuilder builder = new XMLElementBuilder("foo", "http://example.com");
		builder.startInnerElement("bar", "http://other.com");
		assertRendering("<foo xmlns=\"http://example.com\"><bar xmlns=\"http://other.com\"></bar></foo>", builder.build());
	}

	
	public void testRenderInnerNoNamespaceElement() {
		XMLElementBuilder builder = new XMLElementBuilder("foo", "http://example.com");
		builder.startInnerElement("bar");
		assertRendering("<foo xmlns=\"http://example.com\"><bar ></bar></foo>", builder.build());
	}
	
	public void testRenderSimpleText() {
		XMLElement elm = new XMLElement(null, "foo", null, null, new XMLFragment[]{
				new XMLText("bar")
		});
		assertRendering("<foo>bar</foo>", elm);
	}

	public void testRenderTextWithAmpersand() {
		XMLElement elm = new XMLElement(null, "foo", null, null, new XMLFragment[]{
				new XMLText("ba&r")
		});
		assertRendering("<foo>ba&amp;r</foo>", elm);
	}

	public void testRenderTextWithGt() {
		XMLElement elm = new XMLElement(null, "foo", null, null, new XMLFragment[]{
				new XMLText("ba>r")
		});
		assertRendering("<foo>ba&gt;r</foo>", elm);
	}

	public void testRenderTextWithLt() {
		XMLElement elm = new XMLElement(null, "foo", null, null, new XMLFragment[]{
				new XMLText("ba<r")
		});
		assertRendering("<foo>ba&lt;r</foo>", elm);
	}

	private void assertRendering(String expected, XMLElement elm) {
		assertEquals(expected, new Renderer(elm).getComplete());
	}
	
	// TODO test allowed Unicode characters ranged in element name attribute name, attributes values, text
}
