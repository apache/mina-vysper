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

import java.util.Iterator;

import org.apache.vysper.xml.sax.impl.TestHandler.TestEvent;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ParseTextTestCase extends AbstractAsyncXMLReaderTestCase {

    public void testSimpleText() throws Exception {
        Iterator<TestEvent> events = parse("<root>text</root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertText("text", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testEscapedAmp() throws Exception {
        Iterator<TestEvent> events = parse("<root>t&amp;ext</root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertText("t&ext", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testDoubleEscapedAmp() throws Exception {
        Iterator<TestEvent> events = parse("<root>t&amp;amp;ext</root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertText("t&amp;ext", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testUnicodeEscape() throws Exception {
        Iterator<TestEvent> events = parse("<root>t&#4689;ext</root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertText("t\u1251ext", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testUnicodeHexEscape() throws Exception {
        Iterator<TestEvent> events = parse("<root>t&#x1251;ext</root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertText("t\u1251ext", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testConsecutiveUnicodeEscape() throws Exception {
        Iterator<TestEvent> events = parse("<root>&#160;&#160;&#160;&#160;</root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertText("\u00A0\u00A0\u00A0\u00A0", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testTextOnly() throws Exception {
        Iterator<TestEvent> events = parse("text</root>").iterator();

        assertStartDocument(events.next());
        assertFatalError(events.next());

        assertNoMoreevents(events);
    }

}