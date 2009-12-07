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
package org.apache.vysper.xml.decoder;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.decoder.ParticleDecoder;
import org.apache.vysper.xml.decoder.XMLParticle;

/**
 */
public class ParticleDecoderTestCase extends TestCase {

	public void testSimple() throws Exception {
		List<XMLParticle> particles = decodeAll("<root>text</root>");
		XMLParticle opening = particles.get(0);
		XMLParticle text = particles.get(1);
		XMLParticle closing = particles.get(2);
		
		assertTrue(opening.isOpeningElement());
		assertEquals("root", opening.getElementName());
		
		assertTrue(text.isText());
		assertEquals("text", text.getContent());
		
		assertTrue(closing.isClosingElement());
		assertEquals("root", closing.getElementName());
	}

	public void testParseDoubleQuoteAttributes() throws Exception {
		String xml = "<root att=\"foo\">";
		XMLParticle opening = decode(xml);
		
		assertTrue(opening.isOpeningElement());
		assertEquals("root", opening.getElementName());
		assertEquals(xml, opening.getContent());
	}

	public void testParseSingleQuoteAttributes() throws Exception {
		String xml = "<root att='f\"oo'>";
		XMLParticle opening = decode(xml);
		
		assertTrue(opening.isOpeningElement());
		assertEquals("root", opening.getElementName());
		assertEquals(xml, opening.getContent());
	}

	public void testParseAttributeWithLt() throws Exception {
		// TODO This is not supported as per the XML spec, we might want to fail already in ParticleDecoder
		String xml = "<root att='<'>";
		XMLParticle opening = decode(xml);
		
		assertTrue(opening.isOpeningElement());
		assertEquals("root", opening.getElementName());
		assertEquals(xml, opening.getContent());
	}

	public void testParseAttributeWithGt() throws Exception {
		String xml = "<root att='>'>";
		XMLParticle opening = decode(xml);
		assertTrue(opening.isOpeningElement());
		assertEquals("root", opening.getElementName());
		assertEquals(xml, opening.getContent());
	}
	
	public void testParseComment() throws Exception {
		XMLParticle opening = decode("<!-- comment -->");
		
		assertTrue(opening.isOpeningElement());
		// TODO activate when XMLParticle supports comments
		// assertNull(opening.getElementName());
		assertEquals("<!-- comment -->", opening.getContent());
	}

	public void testParsePI() throws Exception {
		String xml = "<?pi att=\"foo\" ?>";
		XMLParticle opening = decode(xml);
		
		assertTrue(opening.isOpeningElement());
		assertEquals("pi", opening.getElementName());
		assertEquals(xml, opening.getContent());
	}

	
	private ByteBuffer wrap(String xml) throws Exception {
		return ByteBuffer.wrap(xml.getBytes("UTF-8"));
	}
	
	private XMLParticle decode(String xml) throws Exception {
		return decode(wrap(xml));
	}

	private XMLParticle decode(ByteBuffer bb) throws Exception {
		return ParticleDecoder.decodeParticle(bb, CharsetUtil.UTF8_DECODER);
	}

	private List<XMLParticle> decodeAll(String xml) throws Exception {
		ByteBuffer bb = wrap(xml);
		List<XMLParticle> particles = new ArrayList<XMLParticle>();
		
		XMLParticle particle = decode(bb);
		while(particle != null) {
			particles.add(particle);
			
			particle = decode(bb);
		}
		return particles; 
	}

}
