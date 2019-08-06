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

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.IN_PROGRESS;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.vysper.compliance.SpecCompliant;

/**
 * provides dates and times in XMPP conformant formats
 */
@SpecCompliant(spec = "XEP-0082", status = IN_PROGRESS, coverage = COMPLETE)
public class DateTimeProfile {

    protected static final TimeZone TIME_ZONE_UTC;

    protected static final FastDateFormat utcDateFormatter;

    protected static final FastDateFormat utcDateTimeFormatter;

    protected static final FastDateFormat utcTimeFormatter;

    private static final String DATE_PATTERN_VALUE = "(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)";

    private static final String TIME_PATTERN_VALUE = "(\\d\\d):(\\d\\d):(\\d\\d)";

    private static final String TZ_PATTERN_VALUE = "(([+-]\\d\\d:\\d\\d)|Z)";

    // time zone is required for date times
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("^" + DATE_PATTERN_VALUE + "T"
            + TIME_PATTERN_VALUE + TZ_PATTERN_VALUE + "$");

    private static final Pattern DATE_PATTERN = Pattern.compile("^" + DATE_PATTERN_VALUE + "$");

    // time zone is optional for times
    private static final Pattern TIME_PATTERN = Pattern.compile("^" + TIME_PATTERN_VALUE + TZ_PATTERN_VALUE + "?$");

    static {
        TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
        utcDateTimeFormatter = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", TIME_ZONE_UTC);
        utcDateFormatter = FastDateFormat.getInstance("yyyy-MM-dd", TIME_ZONE_UTC);
        utcTimeFormatter = FastDateFormat.getInstance("HH:mm:ss'Z'", TIME_ZONE_UTC);
    }

    private final static DateTimeProfile SINGLETON = new DateTimeProfile();

    public static DateTimeProfile getInstance() {
        return SINGLETON;
    }

    protected DateTimeProfile() {
        // empty
    }

    public String getDateTimeInUTC(Date time) {
        return utcDateTimeFormatter.format(time);
    }
    
    public String getDateTimeInUTC(ZonedDateTime dateTime){
        return getDateTimeInUTC(Date.from(dateTime.toInstant()));
    }

    public String getDateInUTC(Date time) {
        return utcDateFormatter.format(time);
    }

    public String getTimeInUTC(Date time) {
        return utcTimeFormatter.format(time);
    }

    /**
     * Parses a date time compliant with ISO-8601 and XEP-0082.
     * @param time The date time string
     * @return A {@link Calendar} representing the date time, in the
     *  time zone specified by the input string
     * @throws IllegalArgumentException If the input string is not a valid date time
     *  e.g. the time zone is missing
     */
    public Calendar fromDateTime(String time) {
        Matcher matcher = DATE_TIME_PATTERN.matcher(time);

        if (matcher.find()) {
            int year = Integer.valueOf(matcher.group(1));
            int month = Integer.valueOf(matcher.group(2));
            int day = Integer.valueOf(matcher.group(3));
            int hour = Integer.valueOf(matcher.group(4));
            int minute = Integer.valueOf(matcher.group(5));
            int second = Integer.valueOf(matcher.group(6));
            String tzValue = matcher.group(7);
            TimeZone tz;
            if (tzValue.equals("Z")) {
                tz = TIME_ZONE_UTC;
            } else {
                tz = TimeZone.getTimeZone("GMT" + tzValue);
            }
            Calendar calendar = Calendar.getInstance(tz);
            calendar.clear();
            calendar.set(year, month - 1, day, hour, minute, second);
            return calendar;
        } else {
            throw new IllegalArgumentException("Invalid date time: " + time);
        }
    }
    
    public ZonedDateTime fromZonedDateTime(String time){
        Calendar calendar = fromDateTime(time);
        return ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
    }

    /**
     * Parses a time, compliant with ISO-8601 and XEP-0082.
     * @param time The time string
     * @return A {@link Calendar} representing the time, in the
     *  time zone specified by the input string. If a time zone is not specified
     *  in the input string, the returned {@link Calendar} will be in the UTC time zone
     * @throws IllegalArgumentException If the input string is not a valid time
     */
    public Calendar fromTime(String time) {
        Matcher matcher = TIME_PATTERN.matcher(time);

        if (matcher.find()) {
            int hour = Integer.valueOf(matcher.group(1));
            int minute = Integer.valueOf(matcher.group(2));
            int second = Integer.valueOf(matcher.group(3));
            String tzValue = matcher.group(4);
            TimeZone tz;
            if (tzValue == null || tzValue.equals("Z")) {
                tz = TIME_ZONE_UTC;
            } else {
                tz = TimeZone.getTimeZone("GMT" + tzValue);
            }
            Calendar calendar = Calendar.getInstance(tz);
            calendar.clear();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, second);
            return calendar;
        } else {
            throw new IllegalArgumentException("Invalid date time: " + time);
        }
    }

    /**
     * Parses a date, compliant with ISO-8601 and XEP-0082.
     * @param time The date string
     * @return A {@link Calendar} representing the date
     * @throws IllegalArgumentException If the input string is not a valid date
     */
    public Calendar fromDate(String time) {
        Matcher matcher = DATE_PATTERN.matcher(time);

        if (matcher.find()) {
            int year = Integer.valueOf(matcher.group(1));
            int month = Integer.valueOf(matcher.group(2));
            int day = Integer.valueOf(matcher.group(3));

            Calendar calendar = Calendar.getInstance(TIME_ZONE_UTC);
            calendar.clear();
            calendar.set(year, month - 1, day);
            return calendar;
        } else {
            throw new IllegalArgumentException("Invalid date time: " + time);
        }
    }
}
