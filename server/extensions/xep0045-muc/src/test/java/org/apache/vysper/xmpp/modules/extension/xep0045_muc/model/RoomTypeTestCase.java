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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import java.util.EnumSet;

import junit.framework.TestCase;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class RoomTypeTestCase extends TestCase {

    public void testComplement() {
        EnumSet<RoomType> original = EnumSet.of(RoomType.FullyAnonymous, RoomType.Hidden);

        EnumSet<RoomType> complemented = RoomType.complement(original);

        assertTrue(complemented.contains(RoomType.FullyAnonymous));
        assertTrue(complemented.contains(RoomType.Hidden));
        assertTrue(complemented.contains(RoomType.Open));
        assertTrue(complemented.contains(RoomType.Unmoderated));
        assertTrue(complemented.contains(RoomType.Unsecured));
        assertTrue(complemented.contains(RoomType.Temporary));

        assertFalse(complemented.contains(RoomType.SemiAnonymous));
        assertFalse(complemented.contains(RoomType.NonAnonymous));
        assertFalse(complemented.contains(RoomType.Public));
        assertFalse(complemented.contains(RoomType.MembersOnly));
        assertFalse(complemented.contains(RoomType.Moderated));
        assertFalse(complemented.contains(RoomType.PasswordProtected));
        assertFalse(complemented.contains(RoomType.Persistent));
    }

    private void assertAntonyms(RoomType type1, RoomType type2) {
        try {
            RoomType.validateAntonyms(EnumSet.of(type1, type2));
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private void assertNonAntonyms(RoomType type1, RoomType type2) {
        RoomType.validateAntonyms(EnumSet.of(type1, type2));
    }

    public void testValidateAntonyms() {
        assertAntonyms(RoomType.Public, RoomType.Hidden);
        assertAntonyms(RoomType.MembersOnly, RoomType.Open);
        assertAntonyms(RoomType.Moderated, RoomType.Unmoderated);
        assertAntonyms(RoomType.Persistent, RoomType.Temporary);
        assertAntonyms(RoomType.Unsecured, RoomType.PasswordProtected);
        assertAntonyms(RoomType.FullyAnonymous, RoomType.SemiAnonymous);
        assertAntonyms(RoomType.SemiAnonymous, RoomType.NonAnonymous);
        assertAntonyms(RoomType.FullyAnonymous, RoomType.NonAnonymous);

        assertNonAntonyms(RoomType.Public, RoomType.Open);
        assertNonAntonyms(RoomType.Unsecured, RoomType.FullyAnonymous);
        assertNonAntonyms(RoomType.Persistent, RoomType.Public);

    }
}
