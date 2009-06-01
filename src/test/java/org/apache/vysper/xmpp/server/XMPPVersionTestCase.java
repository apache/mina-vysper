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

package org.apache.vysper.xmpp.server;

import junit.framework.TestCase;

/**
 */
public class XMPPVersionTestCase extends TestCase {

    public void testParsedVersionOK() {
        assertVersionOK("0.0", "0.0");
        assertVersionOK("0.1", "0.1");
        assertVersionOK("1.0", "1.0");
        assertVersionOK("2.0", "2.0");
        assertVersionOK("2.1", "2.1");
        assertVersionOK("2.11", "2.11");
        assertVersionOK("2.1", "2.01");
        assertVersionOK("2.1", "02.01");
        assertVersionOK("22.1", "022.1");
    }

    public void testParsedVersionNotOK() {
        assertVersionNotOK("-1.0");
        assertVersionNotOK("2.-0");
        assertVersionNotOK("2.01A");
        assertVersionNotOK("02A.01");
    }

    private void assertVersionNotOK(String version) {
        try {
            XMPPVersion xmppVersion = new XMPPVersion(version);
            fail("test failed");
        } catch (Exception e) {
            // test succeeded
        }
    }

    private void assertVersionOK(String versionExpected, String versionInput) {
        XMPPVersion xmppVersion = new XMPPVersion(versionInput);
        assertEquals(versionExpected, xmppVersion.toString());
    }

    public void testCommonDenominator() {
        assertFirstIsCommonDenominator("1.0", "1.1");
        assertFirstIsCommonDenominator("1.1", "1.11");
        assertFirstIsCommonDenominator("1.11", "11.1");
        assertFirstIsCommonDenominator("11.11", "11.111");
        assertFirstIsCommonDenominator("11.001", "11.010");
        assertFirstIsCommonDenominator("11.001", "11.011");
        assertFirstIsCommonDenominator("10.001", "10.011");
        assertFirstIsCommonDenominator("010.001", "10.0011");

    }

    private void assertFirstIsCommonDenominator(String v1, String v2) {
        XMPPVersion version1 = new XMPPVersion(v1);
        XMPPVersion version2 = new XMPPVersion(v2);

        XMPPVersion.getCommonDenomitator(version2, version1);

    }

}
