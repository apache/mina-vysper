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
package org.apache.vysper.xmpp.addressing;

import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.stringprep.StringPrepViolationException;

public class EntityConformanceTestCase extends TestCase {

    public void testCheckRFC3920Conformance() {
        String error = buildLargeString(1024);
        String okButOnTheEdge = buildLargeString(1023);
        runAllChecks(error, "x");
        runAllChecks(error, okButOnTheEdge);
    }

    private void runAllChecks(String error, String ok) {

        assertFalse(doCheck(error, ok, ok));
        assertFalse(doCheck(ok, error, ok));
        assertFalse(doCheck(ok, ok, error));
        assertFalse(doCheck(ok, null, ok));
        assertFalse(doCheck(ok, "", ok));

        assertTrue(doCheck(ok, ok, ok));
        assertTrue(doCheck(null, ok, null));
        assertTrue(doCheck(ok, ok, null));
        assertTrue(doCheck(null, ok, ok));
        assertTrue(doCheck("", ok, ""));
        assertTrue(doCheck(ok, ok, ""));
        assertTrue(doCheck("", ok, ok));
    }

    private boolean doCheck(String node, String domain, String resource) {
        return EntityConformance.checkRFC3920Conformance(new EntityImpl(node, domain, resource));
    }

    private String buildLargeString(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, 'x');
        return new String(chars);
    }

    public void testEquals() {
        assertEquals(new EntityImpl(null, "vysper.org", null), new EntityImpl(null, "vysper.org", ""));
        assertEquals(new EntityImpl(null, "vysper.org", null), new EntityImpl("", "vysper.org", null));
        assertEquals(new EntityImpl(null, "vysper.org", null), new EntityImpl("", "vysper.org", ""));
    }

    public void testPreppedInConstructor() throws EntityFormatException {
        // a colon may not occur in the node part of the JID
        try {
            final EntityImpl testJID = new EntityImpl("contains:colon", "vysper.org", "somebody");
            fail("expected RuntimeException.StringPrepViolationException");
        } catch (RuntimeException rte) {
            assertTrue(rte.getCause() instanceof StringPrepViolationException);
            // test succeeded!
        }
    }
}
