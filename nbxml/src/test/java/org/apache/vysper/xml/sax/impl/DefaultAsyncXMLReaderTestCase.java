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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.xml.sax.DTDHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultAsyncXMLReaderTestCase extends AbstractAsyncXMLReaderTestCase {

    public void testParseAfterFatalError() throws Exception {
        TestHandler handler = new TestHandler();
        NonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        // causes a fatal error
        reader.parse(IoBuffer.wrap("<root></error>".getBytes("UTF-8")), CharsetUtil.getDecoder());

        try {
            // not allowed to parse after an error
            reader.parse(IoBuffer.wrap("<root>".getBytes("UTF-8")), CharsetUtil.getDecoder());
            fail("Must throw SAXException");
        } catch (SAXException e) {
            // OK
        }
    }

    public void testParseAfterEndDocument() throws Exception {
        TestHandler handler = new TestHandler();
        NonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        // causes a fatal error
        reader.parse(IoBuffer.wrap("<root></root>".getBytes("UTF-8")), CharsetUtil.getDecoder());

        try {
            // not allowed to parse after end of document
            reader.parse(IoBuffer.wrap("<root>".getBytes("UTF-8")), CharsetUtil.getDecoder());
            fail("Must throw SAXException");
        } catch (SAXException e) {
            // OK
        }
    }

    public void testSetDtdHandlerNotSupported() {
        try {
            new DefaultNonBlockingXMLReader().setDTDHandler(new DTDHandler() {
                public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
                        throws SAXException {
                }

                public void notationDecl(String name, String publicId, String systemId) throws SAXException {
                }
            });
            fail("Not supported, must throw RuntimeException");
        } catch (RuntimeException e) {
            // OK
        }
    }

    public void testGetDtdHandlerNotSupported() {
        try {
            new DefaultNonBlockingXMLReader().getDTDHandler();
            fail("Not supported, must throw RuntimeException");
        } catch (RuntimeException e) {
            // OK
        }
    }

    public void testSetNotRecognizedFeature() throws SAXNotSupportedException {
        DefaultNonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        try {
            reader.setFeature("http://example.com", true);
            fail("Must throw SAXNotRecognizedException");
        } catch (SAXNotRecognizedException e) {
            // OK
        }
    }

    public void testSetNotSupportedFeature() throws SAXNotRecognizedException {
        DefaultNonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        try {
            reader.setFeature("http://xml.org/sax/features/namespaces", false);
            fail("Must throw SAXNotSupportedException");
        } catch (SAXNotSupportedException e) {
            // OK
        }
    }

    public void testSetFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        DefaultNonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        reader.setFeature("http://xml.org/sax/features/namespaces", true);
    }

    public void testGetFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        DefaultNonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        assertTrue(reader.getFeature("http://xml.org/sax/features/namespaces"));
    }

    public void testGetUnknownFeature() throws SAXNotSupportedException {
        DefaultNonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        try {
            assertTrue(reader.getFeature("http://example.com"));
            fail("Must throw SAXNotRecognizedException");
        } catch (SAXNotRecognizedException e) {
            // ok
        }
    }

    public void testSetFeatureDuringParse() throws Exception {
        DefaultNonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        reader.parse(IoBuffer.wrap("<foo />".getBytes("UTF-8")), CharsetUtil.getDecoder());
        try {
            reader.setFeature("http://xml.org/sax/features/namespaces", true);
            fail("Must throw SAXNotSupportedException");
        } catch (SAXNotSupportedException e) {
            // ok
        }
    }

}
