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

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMPPVersion {

    public static final XMPPVersion VERSION_1_0 = new XMPPVersion(1, 0);

    private int major = 1;

    private int minor = 0;

    private static final String STRING_ZERO = "0";

    private static final String STRING_UNO = "1";

    private static final String STRING_DOT = ".";

    public XMPPVersion() {
        // default values
    }

    public XMPPVersion(int major, int minor) {
        if (major < 0)
            throw new IllegalArgumentException("major must at least be 0");
        this.major = major;
        if (minor < 0)
            throw new IllegalArgumentException("minor must at least be 0");
        this.minor = minor;
    }

    public XMPPVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts == null || parts.length != 2) {
            throw new IllegalArgumentException("XMPP version must be of format 'x.y'");
        }
        major = convertToInt(parts[0]);
        if (major < 0)
            throw new IllegalArgumentException("major must at least be 0");
        minor = convertToInt(parts[1]);
        if (minor < 0)
            throw new IllegalArgumentException("minor must at least be 0");
    }

    private int convertToInt(String part) {
        if (StringUtils.isEmpty(part))
            throw new IllegalArgumentException("version part is empty");
        if (STRING_ZERO.equals(part))
            return 0;
        if (STRING_UNO.equals(part))
            return 1;

        part = part.trim();
        if (part.startsWith("+") || part.startsWith("-"))
            throw new IllegalArgumentException("version part must contain only numbers");
        if (part.startsWith(STRING_ZERO))
            return convertToInt(part.substring(1)); // ignore leading zeros
        return Integer.parseInt(part);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public String toString() {
        return "" + major + STRING_DOT + minor;
    }

    public boolean isUnknownVersion() {
        return major == 0 && minor == 0;
    }

    public static XMPPVersion getCommonDenomitator(XMPPVersion v1, XMPPVersion v2) {
        if (v1.getMajor() != v2.getMajor())
            return v1.getMajor() < v2.getMajor() ? v1 : v2;
        return v1.getMinor() < v2.getMinor() ? v1 : v2;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final XMPPVersion that = (XMPPVersion) o;

        if (major != that.major)
            return false;
        if (minor != that.minor)
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = major;
        result = 29 * result + minor;
        return result;
    }
}
