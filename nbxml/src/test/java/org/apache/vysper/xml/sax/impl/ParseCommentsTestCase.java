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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.vysper.xml.sax.impl.TestHandler.TestEvent;

/**
 * Comments will not generate events, the parser should just support parsing them, unless forbidden
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ParseCommentsTestCase extends AbstractAsyncXMLReaderTestCase {

    public void testCommentFirst() throws Exception {
        Iterator<TestEvent> events = parse("<!-- comment --><root />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testCommentInElement() throws Exception {
        Iterator<TestEvent> events = parse("<root><!-- comment --></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testCommentLast() throws Exception {
        Iterator<TestEvent> events = parse("<root /><!-- comment -->").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testCommentAdvancedContent() throws Exception {
        Iterator<TestEvent> events = parse("<root><!-- 3 comment with multiple words --></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertNoMoreevents(events);
    }

    public void testNotWellformedComment1() throws Exception {
        Iterator<TestEvent> events = parse("<root><!- comment --></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertFatalError(events.next());
        assertNoMoreevents(events);
    }

    public void testNotWellformedComment2() throws Exception {
        Iterator<TestEvent> events = parse("<root><!-- comment -></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertFatalError(events.next());
        assertNoMoreevents(events);
    }

    public void testNotWellformedComment3() throws Exception {
        Iterator<TestEvent> events = parse("<root><!-- comment ></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertFatalError(events.next());
        assertNoMoreevents(events);
    }

    public void testNotWellformedComment4() throws Exception {
        Iterator<TestEvent> events = parse("<root><! comment --></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertFatalError(events.next());
        assertNoMoreevents(events);
    }

    public void testCommentNotAllowed() throws Exception {
        Map<String, Boolean> features = new HashMap<String, Boolean>();
        features.put("http://mina.apache.org/vysper/features/comments-allowed", false);

        Iterator<TestEvent> events = parse("<root><!-- comment --></root>", features, null).iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", events.next());
        assertFatalError(events.next());

        assertNoMoreevents(events);
    }

}