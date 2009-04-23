/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.vysper.xmpp.xmldecoder;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class XMLRawToFragmentConverterConvertErrorTestCase extends TestCase {
    private XMLRawToFragmentConverter xmlRawToFragmentConverter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        xmlRawToFragmentConverter = new XMLRawToFragmentConverter();
    }

    public void testTwoToplevels() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced>"));
        particles.add(new XMLParticle("</balanced>"));
        particles.add(new XMLParticle("<illegal-top-level>"));
        particles.add(new XMLParticle("</illegal-top-level>"));

        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("to consecutive elements are not allowed on top level");
        } catch (DecodingException e) {
            // test succeded
        }
    }
    
    public void testTwoToplevels2() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced>"));
        particles.add(new XMLParticle("<inner />"));
        particles.add(new XMLParticle("</balanced>"));
        particles.add(new XMLParticle("<illegal-top-level>"));
        particles.add(new XMLParticle("</illegal-top-level>"));

        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("to consecutive elements are not allowed on top level");
        } catch (DecodingException e) {
            // test succeded
        }
    }
    
    public void testIllegalOpening() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<start <illegal start>> "));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("opening not allowed inside element");
        } catch (DecodingException e) {
            // test succeded
        }
    }
    
    public void testIllegalOpeningAfterAttr() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<start attr='val' <illegal start>>"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("opening not allowed inside element");
        } catch (DecodingException e) {
            // test succeded
        }
    }
    
    public void testLateOpeningElementName() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("< latename attr='val'>"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("opening xml element name to follow immediately after '<' ");
        } catch (DecodingException e) {
            // test succeded
        }
    }
    
    public void testWrongClosingElementName() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<name attr='val'>"));
        particles.add(new XMLParticle("</nameother >"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("closing xml element name to follow immediately after '</'");
        } catch (DecodingException e) {
            // test succeded
        }
    }
    
    public void testWrongClosingElementWhitespace() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<name attr='val'>"));
        particles.add(new XMLParticle("</ name >"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("closing xml element name to follow immediately after '</'");
        } catch (DecodingException e) {
            // test succeded
        }
    }
    
    public void testWrongClosingElementWhitespace2() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<name attr='val'>"));
        particles.add(new XMLParticle("< /name >"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("closing xml element must start with '</'");
        } catch (DecodingException e) {
            // test succeded
        }
    }

    public void testStartElementEmpty() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        XMLParticle particle = new XMLParticle("< />");
        try {
            String name = particle.getElementName();
            fail("closing xml element must not be empty (direct access)");
        } catch (DecodingException e) {
            // test succeded
        }
        particles.add(particle);
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("closing xml element must not be empty (indirect access)");
        } catch (DecodingException e) {
            // test succeded
        }
    }

    public void testElementNameColon_Only() throws DecodingException {

        XMLParticle particle = new XMLParticle("<: />");
        try {
            String name = particle.getElementName();
            fail("illegal element name ':'");
        } catch (DecodingException e) {
            // test succeded
        }
    }

    public void testElementNameColon_NoName() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        XMLParticle particle = new XMLParticle("<namespace: />");
        particles.add(particle);
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("illegal element name");
        } catch (DecodingException e) {
            // test succeded
        }
    }

    public void testMoreEmptyElements() throws DecodingException {
        try {
            String name = new XMLParticle("<>").getElementName();
            fail("xml element name is mandatory");
        } catch (DecodingException e) {
            // test succeded
        }

        try {
            String name = new XMLParticle("< >").getElementName();
            fail("xml element name is mandatory");
        } catch (DecodingException e) {
            // test succeded
        }

        try {
            String name = new XMLParticle("< attr='k' >").getElementName();
            fail("xml element name is mandatory");
        } catch (DecodingException e) {
            // test succeded
        }

        try {
            String name = new XMLParticle("< attr='k' ></>").getElementName();
            fail("xml element name is mandatory");
        } catch (DecodingException e) {
            // test succeded
        }
    }

    public void testWrongOpeningClosingElementWhitespace2() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<name attr='val' / >"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("opening+closing xml element must end with '/>'");
        } catch (DecodingException e) {
            // test succeded
        }
    }

    public void testAttributeKeyIsNotFollowedByEquals() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<name attr 'val' />"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("attribute and value must be separated by '='");
        } catch (DecodingException e) {
            // test succeded
        }
    }
    
    public void testUnsupportedXMLComment() {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<!-- unsupported comment -->"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("comments are unsupported");
        } catch (UnsupportedXMLException e) {
            // test succeded
        } catch (DecodingException e) {
            fail("more generic exception as expected was thrown");
        }
    }
    
    public void testUnsupportedXMLCommentNested() {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<name attr='val'>"));
        particles.add(new XMLParticle("<!-- unsupported comment -->"));
        particles.add(new XMLParticle("</name >"));
        try {
            xmlRawToFragmentConverter.convert(particles);
            fail("comments are unsupported");
        } catch (UnsupportedXMLException e) {
            // test succeded
        } catch (DecodingException e) {
            fail("more generic exception as expected was thrown");
        }
    }
}