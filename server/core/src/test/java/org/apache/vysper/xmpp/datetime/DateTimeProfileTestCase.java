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
package org.apache.vysper.xmpp.datetime;

import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

@SuppressWarnings("deprecation")
public class DateTimeProfileTestCase extends TestCase {

    private DateTimeProfile dt = DateTimeProfile.getInstance();

    public void testFormatDateTime() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2009, 8, 11, 11, 12, 13);

        String actual = dt.getDateTimeInUTC(cal.getTime());
        assertEquals("2009-09-11T11:12:13Z", actual);
    }

    public void testFormatDate() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2009, 8, 11);

        String actual = dt.getDateInUTC(cal.getTime());
        assertEquals("2009-09-11", actual);
    }

    public void testFormatTime() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.HOUR_OF_DAY, 11);
        cal.set(Calendar.MINUTE, 12);
        cal.set(Calendar.SECOND, 13);

        String actual = dt.getTimeInUTC(cal.getTime());
        assertEquals("11:12:13Z", actual);
    }

    public void testParseDateTimeWithTz() throws Exception {
        Calendar actual = dt.fromDateTime("2009-09-11T11:12:13-01:30");
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("GMT-01:30"));
        expected.clear();
        expected.set(2009, 8, 11, 11, 12, 13);
        assertEquals(expected, actual);
    }

    public void testParseDateTimeWithUTCTz() throws Exception {
        Calendar actual = dt.fromDateTime("2009-09-11T11:12:13Z");
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.clear();
        expected.set(2009, 8, 11, 11, 12, 13);
        assertEquals(expected, actual);
    }

    public void testParseDateTimeWithoutTz() throws Exception {
        try {
            dt.fromDateTime("2009-09-11T11:12:13");
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    public void testParseTimeWithTz() throws Exception {
        Calendar actual = dt.fromTime("11:12:13-01:30");
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("GMT-01:30"));
        expected.clear();
        expected.set(Calendar.HOUR_OF_DAY, 11);
        expected.set(Calendar.MINUTE, 12);
        expected.set(Calendar.SECOND, 13);

        assertEquals(expected, actual);
    }

    public void testParseTimeWithUTCTz() throws Exception {
        Calendar actual = dt.fromTime("11:12:13Z");
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.clear();
        expected.set(Calendar.HOUR_OF_DAY, 11);
        expected.set(Calendar.MINUTE, 12);
        expected.set(Calendar.SECOND, 13);
        assertEquals(expected, actual);
    }

    public void testParseTimeWithoutTz() throws Exception {
        Calendar actual = dt.fromTime("11:12:13");
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.clear();
        expected.set(Calendar.HOUR_OF_DAY, 11);
        expected.set(Calendar.MINUTE, 12);
        expected.set(Calendar.SECOND, 13);
        assertEquals(expected, actual);
    }

    public void testParseDate() throws Exception {
        Calendar actual = dt.fromDate("2009-09-11");
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.clear();
        expected.set(2009, 8, 11);
        assertEquals(expected, actual);
    }

}
