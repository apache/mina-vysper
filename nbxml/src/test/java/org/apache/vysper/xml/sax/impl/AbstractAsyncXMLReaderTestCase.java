/*
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
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.TestHandler.CharacterEvent;
import org.apache.vysper.xml.sax.impl.TestHandler.EndDocumentEvent;
import org.apache.vysper.xml.sax.impl.TestHandler.EndElementEvent;
import org.apache.vysper.xml.sax.impl.TestHandler.FatalErrorEvent;
import org.apache.vysper.xml.sax.impl.TestHandler.StartDocumentEvent;
import org.apache.vysper.xml.sax.impl.TestHandler.StartElementEvent;
import org.apache.vysper.xml.sax.impl.TestHandler.TestEvent;
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

    protected Attributes attributes(Attribute... attributes) {
        List<Attribute> list = new ArrayList<Attribute>();
        for (Attribute attribute : attributes) {
            list.add(attribute);
        }
        return new DefaultAttributes(list);
    }

    protected void assertStartElement(String expectedUri, String expectedLocalName, String expectedQName,
            Attributes expectedAttributes, TestEvent actual) {
        printIfFatal(actual);
        if (!(actual instanceof StartElementEvent))
            fail("Event must be StartElementEvent but was " + actual.getClass());
        StartElementEvent startElementEvent = (StartElementEvent) actual;
        assertEquals("URI", expectedUri, startElementEvent.getURI());
        assertEquals("local name", expectedLocalName, startElementEvent.getLocalName());
        assertEquals("qName", expectedQName, startElementEvent.getQName());
        assertAttributes(expectedAttributes, startElementEvent.getAtts());
    }

    private void printIfFatal(TestEvent actual) {
        if (actual instanceof FatalErrorEvent) {
            ((FatalErrorEvent) actual).getException().printStackTrace();
        }

    }

    protected void assertAttributes(Attributes expectedAttrs, Attributes actualAttrs) {
        assertEquals("Attribute count", expectedAttrs.getLength(), actualAttrs.getLength());

        for (int i = 0; i < expectedAttrs.getLength(); i++) {
            int actualIndex = actualAttrs.getIndex(expectedAttrs.getQName(i));
            assertTrue("Actual attribute not found for QName " + expectedAttrs.getQName(i), actualIndex > -1);

            assertEquals("Local name[" + i + "]", expectedAttrs.getLocalName(i), actualAttrs.getLocalName(actualIndex));
            assertEquals("Qname[" + i + "]", expectedAttrs.getQName(i), actualAttrs.getQName(actualIndex));
            assertEquals("URI[" + i + "]", expectedAttrs.getURI(i), actualAttrs.getURI(actualIndex));
            assertEquals("Value[" + i + "]", expectedAttrs.getValue(i), actualAttrs.getValue(actualIndex));
        }
    }

    protected void assertEndElement(String expectedUri, String expectedLocalName, String expectedQName, TestEvent actual) {
        printIfFatal(actual);
        if (!(actual instanceof EndElementEvent))
            fail("Event must be EndElementEvent");
        EndElementEvent endElementEvent = (EndElementEvent) actual;
        assertEquals("URI", expectedUri, endElementEvent.getURI());
        assertEquals("local name", expectedLocalName, endElementEvent.getLocalName());
        assertEquals("qName", expectedQName, endElementEvent.getQName());
    }

    protected void assertStartDocument(TestEvent actual) {
        printIfFatal(actual);
        if (!(actual instanceof StartDocumentEvent))
            fail("Event must be StartDocumentEvent but is " + actual.getClass());
    }

    protected void assertEndDocument(TestEvent actual) {
        printIfFatal(actual);
        if (!(actual instanceof EndDocumentEvent))
            fail("Event must be EndDocumentEvent");
    }

    protected void assertText(String expected, TestEvent actual) {
        printIfFatal(actual);
        if (!(actual instanceof CharacterEvent))
            fail("Event must be CharacterEvent");

        assertEquals(expected, ((CharacterEvent) actual).getCharacters());
    }

    protected List<TestEvent> parse(String xml) throws Exception {
        return parse(xml, null, null);
    }

    protected List<TestEvent> parse(String xml, Map<String, Boolean> features, Map<String, Object> properties)
            throws Exception {
        TestHandler handler = new TestHandler();
        NonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        if (features != null) {
            for (Entry<String, Boolean> feature : features.entrySet()) {
                reader.setFeature(feature.getKey(), feature.getValue());
            }
        }
        if (properties != null) {
            for (Entry<String, Object> property : properties.entrySet()) {
                reader.setProperty(property.getKey(), property.getValue());
            }
        }

        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        reader.parse(IoBuffer.wrap(xml.getBytes("UTF-8")), CharsetUtil.getDecoder());

        return handler.getEvents();
    }

    protected void assertFatalError(TestEvent actual) {
        if (!(actual instanceof FatalErrorEvent))
            fail("Event must be FatalErrorEvent but is " + actual.getClass());
    }

    protected void assertNoMoreevents(Iterator events) {
        if (events.hasNext()) {
            fail("Must not be any more evens, but found one " + events.next().getClass());
        }
    }
}
