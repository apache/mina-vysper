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
package org.apache.vysper.xmpp.xmlfragment;

import junit.framework.TestCase;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 */
public class XMLElementTestCase extends TestCase {

    public void testBasicGetters() {
        XMLElement xmlElement = new StanzaBuilder("message", "jabber:test").
                addAttribute("lang", "de").
                addAttribute("xml:lang", "cn").
                addAttribute("xmllang", "en").
        getFinalStanza();
        
        assertEquals("message", xmlElement.getName());
        assertEquals("jabber:test", xmlElement.getNamespace());
        assertEquals("cn", xmlElement.getXMLLang());
        
        assertSame(Collections.emptyList(), xmlElement.getInnerElements());
        List<Attribute> list = xmlElement.getAttributes();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    public void testInnerTextGetters() {
        
        XMLElement xmlElement = new StanzaBuilder("message", "jabber:test").
                addText("t1").
                startInnerElement("i1").
                endInnerElement().
                addText("t2").
                addText("t3").
                startInnerElement("i2").
                endInnerElement().
                addText("t4").
        getFinalStanza();

        List<XMLText> list = xmlElement.getInnerTexts();
        assertEquals(4, list.size());
        assertEquals("t1", list.get(0).getText());
        assertEquals("t2", list.get(1).getText());
        assertEquals("t3", list.get(2).getText());
        assertEquals("t4", list.get(3).getText());

        assertEquals("t1", xmlElement.getFirstInnerText().getText());
        try {
            xmlElement.getSingleInnerText();
            fail("must raise exception");
        } catch (XMLSemanticError xmlSemanticError) {
            // test succeeded
        }

        xmlElement = new StanzaBuilder("message", "jabber:test").
                startInnerElement("i1").
                endInnerElement().
        getFinalStanza();
        try {
            assertNull(xmlElement.getSingleInnerText());
        } catch (XMLSemanticError xmlSemanticError) {
            fail("must not raise error");
        }
    }

    public void testInnerElementGetters() {
        
        XMLElement xmlElement = new StanzaBuilder("message", "jabber:test").
                addText("t1").
                startInnerElement("i1").
                endInnerElement().
                startInnerElement("i2").
                    addAttribute("order", "1").
                endInnerElement().
                startInnerElement("i2").
                    addAttribute("order", "2").
                endInnerElement().
                addText("t2").
                addText("t3").
                startInnerElement("i3").
                endInnerElement().
                addText("t4").
        getFinalStanza();

        List<XMLElement> list = xmlElement.getInnerElements();
        assertEquals(4, list.size());

        assertEquals("i1", xmlElement.getFirstInnerElement().getName());
        try {
            xmlElement.getSingleInnerElementsNamed("i2");
            fail("must raise exception");
        } catch (XMLSemanticError xmlSemanticError) {
            // test succeeded
        }

        try {
            XMLElement xmlElement1 = xmlElement.getSingleInnerElementsNamed("i3");
            assertEquals("i3", xmlElement1.getName());
        } catch (XMLSemanticError xmlSemanticError) {
            fail("must not raise exception");
        }

        xmlElement = new StanzaBuilder("message", "jabber:test").
                addText("t1").
        getFinalStanza();
        try {
            assertNull(xmlElement.getSingleInnerElementsNamed("none"));
        } catch (XMLSemanticError xmlSemanticError) {
            fail("must not raise error");
        }

        xmlElement = new StanzaBuilder("message", "jabber:test").
                startInnerElement("i").
                    addAttribute("order", "1").
                endInnerElement().
                startInnerElement("another").
                endInnerElement().
        getFinalStanza();
        try {
            XMLElement singleXmlElement = xmlElement.getSingleInnerElementsNamed("i");
            assertEquals("i", singleXmlElement.getName());
        } catch (XMLSemanticError xmlSemanticError) {
            fail("must not raise error");
        }

        xmlElement = new StanzaBuilder("message", "jabber:test").
                startInnerElement("i").
                    addAttribute("order", "1").
                endInnerElement().
                startInnerElement("i").
                    addAttribute("order", "2").
                endInnerElement().
        getFinalStanza();
        try {
            xmlElement.getSingleInnerElementsNamed("i");
            fail("must raise error, more than one i-element");
        } catch (XMLSemanticError xmlSemanticError) {
            // test succeeded
        }
    }
    
    public void testInnerElementsNamed() {
        
        XMLElement xmlElement = new StanzaBuilder("message", "jabber:test").
                addText("t1").
                startInnerElement("body").
                endInnerElement().
                startInnerElement("body").
                    addAttribute("order", "1").
                endInnerElement().
                startInnerElement("body").
                    addAttribute("order", "2").
                endInnerElement().
                addText("body").
                addText("t3").
                startInnerElement("single").
                endInnerElement().
                addText("t4").
        getFinalStanza();

        List<XMLElement> list = xmlElement.getInnerElementsNamed("no-exist");
        assertEquals(0, list.size());

        list = xmlElement.getInnerElementsNamed("single");
        assertEquals(1, list.size());

        try {
            XMLElement xmlElementInner = xmlElement.getSingleInnerElementsNamed("single");
            assertNotNull(xmlElementInner);
        } catch (XMLSemanticError xmlSemanticError) {
            fail("single element not found");
        }

        List<XMLElement> named = xmlElement.getInnerElementsNamed("body");
        assertEquals(3, named.size());
        
        try {
            xmlElement.getSingleInnerElementsNamed("body");
            fail("no single element");
        } catch (XMLSemanticError xmlSemanticError) {
            // success
        }
    }
    
    public void testLanguageMapping() {
        
        XMLElement xmlElement = new StanzaBuilder("message", "jabber:test").
                addText("t1").
                startInnerElement("body").
                endInnerElement().
                startInnerElement("body").
                    addAttribute("xml:lang", "en").
                endInnerElement().
                startInnerElement("body").
                    addAttribute("xml:lang", "de").
                endInnerElement().
                addText("body").
                addText("t3").
                startInnerElement("single").
                    addAttribute("xml:lang", "ru").
                endInnerElement().
                startInnerElement("body_inconsistent").
                    addAttribute("xml:lang", "ru").
                endInnerElement().
                startInnerElement("body_inconsistent").
                    addAttribute("xml:lang", "ru").
                endInnerElement().
                startInnerElement("body_lang_null").
                    addAttribute("order", "1").
                endInnerElement().
                startInnerElement("body_lang_null").
                    addAttribute("order", "2").
                endInnerElement().
                addText("t4").
        getFinalStanza();

        try {
            Map<String,XMLElement> map = xmlElement.getInnerElementsByXMLLangNamed("body");
            assertEquals(3, map.size());
            XMLElement element = map.get("en");
            assertEquals("en", element.getXMLLang());
            element = map.get("de");
            assertEquals("de", element.getXMLLang());
            element = map.get(null);
            assertEquals(null, element.getXMLLang());
            
        } catch (XMLSemanticError xmlSemanticError) {
            fail("no error expected");
        }
        
        try {
            Map<String,XMLElement> map = xmlElement.getInnerElementsByXMLLangNamed("body_lang_null");
            fail("semantic error expected, same language occurs 2 times");
        } catch (XMLSemanticError xmlSemanticError) {
            // success
        }
        try {
            Map<String,XMLElement> map = xmlElement.getInnerElementsByXMLLangNamed("body_lang_null");
            fail("semantic error expected, language does not occur 2 times");
        } catch (XMLSemanticError xmlSemanticError) {
            // success
        }
    }
}
