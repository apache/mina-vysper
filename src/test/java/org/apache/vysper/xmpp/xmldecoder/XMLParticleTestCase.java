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
package org.apache.vysper.xmpp.xmldecoder;

import junit.framework.TestCase;

/**
 */
public class XMLParticleTestCase extends TestCase {

    public void testOpeningElementName() throws DecodingException {
        assertOpeningElementName("<stream>", "stream");
        assertOpeningElementName("<!stream>", "stream");
        assertOpeningElementName("<stream!>", "stream");
        assertOpeningElementName("<?stream?>", "stream");
        assertOpeningElementName("<?stream ?>", "stream");
        assertOpeningElementName("<stream />", "stream");
        assertOpeningElementName("<stream:stream />", "stream:stream");
        assertOpeningElementName("<stream:stream x=\"x\" />", "stream:stream");
        assertOpeningElementName("<stream:stream x=\'x\' />", "stream:stream");
        assertOpeningElementName("<:colon-prefixed>", ":colon-prefixed");
        assertOpeningElementName("<wrong.separator>", "wrong.separator");
        assertOpeningElementName("<wrong_separator>", "wrong_separator");
    }
    
    public void testWrongOpeningElementName() {
        assertFailureOpeningElementName("<>");
        assertFailureOpeningElementName("<&()\u00c2\u00a7$%/>");
        assertFailureOpeningElementName("< space-prefixed />");
        assertFailureOpeningElementName("<-prefixed>");
    } 

    public void testMoreWrongOpeningElementName() {
        assertFailureOpeningElementName("<wrong#separator>");
    } 

    private void assertOpeningElementName(String input, String expected) throws DecodingException {
        XMLParticle particle = new XMLParticle(input);
        assertEquals(expected, particle.getElementName());
    }

    private void assertFailureOpeningElementName(String input) {
        XMLParticle particle = new XMLParticle(input);
        try {
            particle.getElementName();
            fail("failure expected");
        } catch (DecodingException e) {
            // expected
        }
    }
}
