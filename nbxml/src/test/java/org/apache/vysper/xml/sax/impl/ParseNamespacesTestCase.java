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
public class ParseNamespacesTestCase extends AbstractAsyncXMLReaderTestCase {

    public void testNamespacedElement() throws Exception {
        Iterator<TestEvent> events = parse("<root xmlns='urn:test'></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("urn:test", "root", "root", events.next());
        assertEndElement("urn:test", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testNamespacedAttribute() throws Exception {
        Iterator<TestEvent> events = parse("<root p:att='foo' xmlns:p='urn:test'></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("", "root", "root", attributes(new Attribute("att", "urn:test", "p:att", "foo")), events
                .next());
        assertEndElement("", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testDefaultedAttribute() throws Exception {
        Iterator<TestEvent> events = parse("<root att='foo' xmlns='urn:test'></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("urn:test", "root", "root", attributes(new Attribute("att", "", "att", "foo")), events
                .next());
        assertEndElement("urn:test", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testSimpleQNameElement() throws Exception {
        Iterator<TestEvent> events = parse("<p:root xmlns:p='urn:test'></p:root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("urn:test", "root", "p:root", events.next());
        assertEndElement("urn:test", "root", "p:root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testNamespacedInheritanceElement() throws Exception {
        Iterator<TestEvent> events = parse("<root xmlns='urn:test'><child /></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("urn:test", "root", "root", events.next());
        assertStartElement("urn:test", "child", "child", events.next());
        assertEndElement("urn:test", "child", "child", events.next());
        assertEndElement("urn:test", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testOverrideNamespacedElement() throws Exception {
        Iterator<TestEvent> events = parse("<root xmlns='urn:test'><child xmlns='urn:child' /></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("urn:test", "root", "root", events.next());
        assertStartElement("urn:child", "child", "child", events.next());
        assertEndElement("urn:child", "child", "child", events.next());
        assertEndElement("urn:test", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testResetNamespacedElement() throws Exception {
        Iterator<TestEvent> events = parse("<root xmlns='urn:test'><child xmlns='' /></root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("urn:test", "root", "root", events.next());
        assertStartElement("", "child", "child", events.next());
        assertEndElement("", "child", "child", events.next());
        assertEndElement("urn:test", "root", "root", events.next());
        assertEndDocument(events.next());

        assertFalse(events.hasNext());
    }

    public void testInvalidPrefixElement() throws Exception {
        Iterator<TestEvent> events = parse("<p1:root xmlns:p1='urn:test' xmlns:p2='urn:test'></p2:root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("urn:test", "root", "p1:root", events.next());
        assertFatalError(events.next());

        assertFalse(events.hasNext());
    }

    public void testUnknownPrefixOnElement() throws Exception {
        Iterator<TestEvent> events = parse("<p:root />").iterator();

        assertStartDocument(events.next());
        assertFatalError(events.next());

        assertFalse(events.hasNext());
    }

    public void testUnknownPrefixOnAttribute() throws Exception {
        Iterator<TestEvent> events = parse("<root p:att='foo' />").iterator();

        assertStartDocument(events.next());
        assertFatalError(events.next());

        assertFalse(events.hasNext());
    }

    public void testInvalidNamespaceElement() throws Exception {
        Iterator<TestEvent> events = parse("<p1:root xmlns:p1='urn:test' xmlns:p2='urn:foo'></p2:root>").iterator();

        assertStartDocument(events.next());
        assertStartElement("urn:test", "root", "p1:root", events.next());
        assertFatalError(events.next());

        assertFalse(events.hasNext());
    }

}