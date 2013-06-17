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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.TestHandler.CharacterEvent;
import org.apache.vysper.xml.sax.impl.TestHandler.TestEvent;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ParseElementsTestCase extends AbstractAsyncXMLReaderTestCase {

    public void testEmptyElement() throws Exception {
        Iterator<TestEvent> events = parse("<root />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testElement() throws Exception {
        Iterator<TestEvent> events = parse("<root></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testElements() throws Exception {
        Iterator<TestEvent> events = parse("<root><child><inner /></child></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertStartElement("", "child", "child", events.next());
        assertStartElement("", "inner", "inner", events.next());

        assertEndElement("", "inner", "inner", events.next());
        assertEndElement("", "child", "child", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testIllegalClosingElement() throws Exception {
        Iterator<TestEvent> events = parse("<root><child /></error>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertStartElement("", "child", "child", events.next());
        assertEndElement("", "child", "child", events.next());
        assertFatalError(events.next());

        assertNoMoreevents(events);
    }

    public void testNumberAsFirstCharInName() throws Exception {
        Iterator<TestEvent> events = parse("<1root />").iterator();

        assertStartDocument(events.next());
        assertFatalError(events.next());

        assertNoMoreevents(events);
    }

    public void testDashAsFirstCharInName() throws Exception {
        Iterator<TestEvent> events = parse("<-root />").iterator();

        assertStartDocument(events.next());
        assertFatalError(events.next());

        assertNoMoreevents(events);
    }

    public void testNumberInName() throws Exception {
        Iterator<TestEvent> events = parse("<r1oot />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "r1oot", "r1oot", events.next());
        assertEndElement("", "r1oot", "r1oot", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testDashInName() throws Exception {
        Iterator<TestEvent> events = parse("<ro-ot />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "ro-ot", "ro-ot", events.next());
        assertEndElement("", "ro-ot", "ro-ot", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testInvalidUnicodeInName() throws Exception {
        Iterator<TestEvent> events = parse("<r\u2190oot />").iterator();

        assertStartDocument(events.next());
        assertFatalError(events.next());

        assertNoMoreevents(events);
    }

    public void testValidUnicodeInName() throws Exception {
        Iterator<TestEvent> events = parse("<r\u218Foot />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "r\u218Foot", "r\u218Foot", events.next());
        assertEndElement("", "r\u218Foot", "r\u218Foot", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testXmlBeginName() throws Exception {
        Iterator<TestEvent> events = parse("<xmlroot />").iterator();

        assertStartDocument(events.next());
        assertFatalError(events.next());

        assertNoMoreevents(events);
    }

    public void testXmlInsideName() throws Exception {
        Iterator<TestEvent> events = parse("<roxmlot />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "roxmlot", "roxmlot", events.next());
        assertEndElement("", "roxmlot", "roxmlot", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testMixedXmlBeginName() throws Exception {
        Iterator<TestEvent> events = parse("<XmLroot />").iterator();

        assertStartDocument(events.next());
        assertFatalError(events.next());

        assertNoMoreevents(events);
    }

    public void testSplitBuffers() throws Exception {
        TestHandler handler = new TestHandler();
        NonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();

        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        String s = "<root>\u1251</root>";

        // split in the middle of the Unicode char
        byte[] xml = s.getBytes("UTF-8");
        byte[] xml1 = new byte[8];
        byte[] xml2 = new byte[8];

        System.arraycopy(xml, 0, xml1, 0, 8);
        System.arraycopy(xml, 8, xml2, 0, 8);

        reader.parse(IoBuffer.wrap(xml1), CharsetUtil.getDecoder());
        reader.parse(IoBuffer.wrap(xml2), CharsetUtil.getDecoder());

        Iterator<TestEvent> events = handler.getEvents().iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertText("\u1251", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

}
