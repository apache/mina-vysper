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
public class ParseAttributesTestCase extends AbstractAsyncXMLReaderTestCase {

    public void testSimpleAttribute() throws Exception {
        Iterator<TestEvent> events = parse("<root att='foo' />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", attributes(new Attribute("att", "", "att", "foo")), events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testMultipleAttribute() throws Exception {
        Iterator<TestEvent> events = parse("<root att='foo' att2='bar' />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", attributes(new Attribute("att", "", "att", "foo"), new Attribute("att2",
                "", "att2", "bar")), events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testAttributeWithDoubleQuote() throws Exception {
        Iterator<TestEvent> events = parse("<root att='f\"oo' />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", attributes(new Attribute("att", "", "att", "f\"oo")), events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testAttributeWithSingleQuote() throws Exception {
        Iterator<TestEvent> events = parse("<root att=\"f'oo\" />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", attributes(new Attribute("att", "", "att", "f'oo")), events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testAttributeWithEscapedAmp() throws Exception {
        Iterator<TestEvent> events = parse("<root att='f&amp;oo' />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", attributes(new Attribute("att", "", "att", "f&oo")), events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    // Namespace declarations should not be included in attribute
    public void testExcludeNsAttributes() throws Exception {
        Iterator<TestEvent> events = parse("<root att='foo' xmlns:p='http://bar.com' />").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", attributes(new Attribute("att", "", "att", "foo")), events.next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

}