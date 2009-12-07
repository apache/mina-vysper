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
package org.apache.vysper.xmpp.parser;

import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.stanza.Stanza;

import java.util.List;

/**
 * testing all implementations of StreamParser
 * @see org.apache.vysper.xmpp.parser.StreamParser
 */
public abstract class AbstractStreamParserTestCase extends TestCase {

    /**
     * creates the StreamParser implementation to be tested.
     * it must parse the provided xml string
     */
    protected abstract StreamParser createStreamParser(String xml);

    /**
     * convinience method to create the tested stream parser and
     * only return the first stanza from the given xml
     */
    protected abstract Stanza getFirstStanzaFromXML(String xml) throws ParsingException;

    public void testSimple() throws ParsingException {
        Stanza stanza = getFirstStanzaFromXML("<test>leeklf</test>");
        assertEquals("stanza simple text", "test", stanza.getName());
        assertNotNull("empty attr", stanza.getAttributes());
        assertEquals("zero attr", 0, stanza.getAttributes().size());

        stanza = getFirstStanzaFromXML("<test2/><next>...");
        assertEquals("stanza simple text2", "test2", stanza.getName());
    }

    public void testSequenceOfTwo() throws ParsingException {
        assertSequenceOfTwo("<test_uno>payload_1</test_uno><test_due>payload_2</test_due>");
        assertSequenceOfTwo("<test_uno><inner><inner_x></inner_x>payload_1</inner></test_uno><test_due><inner/>payload_2</test_due>");
    }

    public void testSequenceOfTwo_RestrictedXML() throws ParsingException {
        // restricted XML must be ignored (RFC3920 section 11.1)
        assertSequenceOfTwo("<test_uno></test_uno>NOT ALLOWED TEXT BETWEEN STANZAS<test_due></test_due>");
        assertSequenceOfTwo("<test_uno></test_uno><!--NOT ALLOWED COMMENT BETWEEN STANZAS --><test_due></test_due>");
    }

    private void assertSequenceOfTwo(String xml) throws ParsingException {
        StreamParser stringStreamParser = createStreamParser(xml);
        Stanza stanza = stringStreamParser.getNextStanza();
        assertEquals("stanza 1", "test_uno", stanza.getName());
        stanza = stringStreamParser.getNextStanza();
        assertEquals("stanza 2", "test_due", stanza.getName());
        stanza = stringStreamParser.getNextStanza();
        assertNull("stanza 3 not existing", stanza);
    }

    public void testBalancedElements() throws ParsingException {
        getFirstStanzaFromXML("<test>leeklf</test>");

        getFirstStanzaFromXML("<test>leeklf<inner></inner></test>");

        getFirstStanzaFromXML("<test>leeklf<inner/></test>");

        try {
            getFirstStanzaFromXML("<test>leeklf<inner></test>");
            fail("must raise exception");
        } catch (Exception e) {
            // fall thru
        }

        Stanza stanza = getFirstStanzaFromXML("<test>leeklf<test></test></test>");
        assertEquals("inners", 2, stanza.getInnerFragments().size());
        assertEquals("inner w/same name", "test", ((XMLElement) stanza.getInnerFragments().get(1)).getName());
    }

    public void testAttributes() throws ParsingException {
        String xml = "<testAttr at1=\"av1\" at2=\"av2\" />";
        Stanza stanza = getFirstStanzaFromXML(xml);
        assertEquals("attributes length", 2, stanza.getAttributes().size());
        assertEquals("stanza name", "testAttr", stanza.getName());
        List<Attribute> attributes = stanza.getAttributes();

        // inner attribues are immutable
        int size = attributes.size();
        try {
            attributes.add(new Attribute("not", "insertable"));
            fail("attributes should be immutable");
        } catch (UnsupportedOperationException e) {
            // succeeded
        }
        assertEquals("nothing inserted", size, attributes.size());

        assertEquals("a1", "at1", attributes.get(0).getName());
        assertEquals("a1", "av1", attributes.get(0).getValue());
        assertEquals("a2", "at2", attributes.get(1).getName());
        assertEquals("a2", "av2", attributes.get(1).getValue());
        try {
            attributes.add(new Attribute("unmodName", "unmodValue"));
            fail("could modify mutual attribute list");
        } catch (UnsupportedOperationException e) {
            // fall through
        }
    }

    public void testNestedFragments() throws ParsingException {
        String xml = "<test_uno><inner><inner_x></inner_x>payload_1</inner>payload_2</test_uno>";
        Stanza stanza = getFirstStanzaFromXML(xml);

        List<XMLFragment> innerFragments = stanza.getInnerFragments();

        // inner frags are immutable
        int size = innerFragments.size();
        try {
            innerFragments.add(new XMLText("not insertable"));
            fail("fragments should be immutable");
        } catch (UnsupportedOperationException e) {
            // succeeded
        }
        assertEquals("nothing inserted", size, innerFragments.size());

        XMLFragment xmlFragment = innerFragments.get(0);
        assertInnerXMLElement(xmlFragment, "inner", 2);

        XMLFragment xmlFragmentDeep = ((XMLElement)xmlFragment).getInnerFragments().get(0);
        assertInnerXMLElement(xmlFragmentDeep, "inner_x", 0);

        xmlFragment = innerFragments.get(1);
        assertInnerTextElement(xmlFragment, "payload_2");

    }

    private void assertInnerTextElement(XMLFragment xmlFragment, String text) {
        assertTrue(xmlFragment instanceof XMLText);
        XMLText xmlText = ((XMLText) xmlFragment);
        assertEquals("text", text, xmlText.getText());
    }

    private void assertInnerXMLElement(XMLFragment xmlFragment, String elementName, int numberOfSubelements) {
        assertTrue(xmlFragment instanceof XMLElement);
        XMLElement xmlElement = ((XMLElement) xmlFragment);
        assertEquals("elementName", elementName, xmlElement.getName());
        assertEquals("subelements", numberOfSubelements, xmlElement.getInnerFragments().size());
    }

    public void testStartStanza() throws ParsingException {
        String sequence = "<stream:stream\n" +
                            "    to='example.com'\n" +
                            "    xmlns='jabber:client'\n" +
                            "    xmlns:stream='http://etherx.jabber.org/streams'\n" +
                            "    version='1.0' />";
        Stanza stanza = getFirstStanzaFromXML(sequence);
        String name = stanza.getName();
        assertEquals("name", "stream", name);
    }

    public void testXMLHeader() throws ParsingException {
        String xml =
        "<?xml version='1.0'?><stream:stream from='example.com' id='someid' " +
        " xmlns='jabber:client'" +
        " xmlns:stream='http://etherx.jabber.org/streams' version='1.0' />";

        
        Stanza stanza = getFirstStanzaFromXML(xml);
        assertEquals("stream start", "stream", stanza.getName());
    }
}
