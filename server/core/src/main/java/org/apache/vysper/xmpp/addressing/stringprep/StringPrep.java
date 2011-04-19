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
package org.apache.vysper.xmpp.addressing.stringprep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.vysper.compliance.SpecCompliant;

/**
 * Use this class to prepare a String according to the Stringprep profile. The
 * methods {@link #buildMapping()} and {@link #buildProhibitedSet()} can be
 * overridden to modify the behavior of the Stringprep check.
 * 
 * see http://www.ietf.org/rfc/rfc3454.txt
 * 
 * @author Gerolf Seitz (gseitz@apache.org)
 * 
 */
@SpecCompliant(spec = "RFC3454")
public class StringPrep {

    private Map<String, String> mapping;

    private Set<String> prohibited;

    /**
     * Construct.
     */
    public StringPrep() {
        mapping = buildMapping();
        prohibited = buildProhibitedSet();
    }

    /**
     * Prepares the given {@link String} according to the Stringprep
     * specification.
     * 
     * @param str
     *            the string to prepare
     * @return the prepared {@link String}
     * @throws StringPrepViolationException
     *             in case the {@link String} cannot be prepared
     */
    public String prepareString(String str) throws StringPrepViolationException {
        // 1. map -> RFC3454:3
        for (int i = 0; i < str.length(); i++) {
            String codePoint = codePointAt(str, i);
            if (mapping.containsKey(codePoint)) {
                str = str.replace(codePoint, mapping.get(codePoint));
            }
        }
        // TODO: 2. normalize -> RFC3454:4

        // 3. prohibit -> RFC3454:5
        for (int i = 0; i < str.length(); i++) {
            String codePoint = codePointAt(str, i);
            if (prohibited.contains(codePoint)) {
                throw new StringPrepViolationException(String.format("character '%s' prohibited!", codePoint));
            }
        }

        // 4. check bidi -> RFC3454:6
        boolean containsRAndAlCat = false;
        boolean containsLCat = false;
        for (int i = 0; i < str.length() && NAND(containsRAndAlCat, containsLCat); i++) {
            String codePoint = codePointAt(str, i);
            containsRAndAlCat |= StringPrepConstants.D_1_CharactersWithBiDiPropertiesRorAl.contains(codePoint);
            containsLCat |= StringPrepConstants.D_2_CharactersWithBiDiPropertyL.contains(codePoint);
        }
        if (containsRAndAlCat && containsLCat) {
            throw new StringPrepViolationException("invalid bidi sequence");
        }
        if (containsRAndAlCat) {
            if (!StringPrepConstants.D_1_CharactersWithBiDiPropertiesRorAl.contains(codePointAt(str, 0))
                    || !StringPrepConstants.D_1_CharactersWithBiDiPropertiesRorAl.contains(codePointAt(str, str
                            .length() - 1))) {
                throw new StringPrepViolationException("invalid bidi sequence");
            }
        }

        return str;
    }

    /**
     * Override this method and return a custom map of character mappings to
     * alter the Stringprep behavior.
     * 
     * @return a {@link Map}<String, String> containing all character mappings
     */
    protected Map<String, String> buildMapping() {
        Map<String, String> mapping = new HashMap<String, String>();
        mapping.putAll(StringPrepConstants.B_1_CommonlyMappedtoNothing);
        mapping.putAll(StringPrepConstants.B_2_MappingForCaseFoldingUsedWithKFC);
        mapping.putAll(StringPrepConstants.B_3_MappingForCaseFoldingWithNoNormalization);

        return mapping;
    }

    /**
     * Override this method and return a custom set of prohibited characters to
     * alter the Stringprep behavior.
     * 
     * @return a {@link Set}<String> containing all characters that are
     *         prohibited
     */
    protected Set<String> buildProhibitedSet() {
        Set<String> prohibited = new HashSet<String>();
        prohibited.addAll(StringPrepConstants.C_1_1_AsciiSpaceCharacters);
        prohibited.addAll(StringPrepConstants.C_1_2_NonAsciiSpaceCharacters);
        prohibited.addAll(StringPrepConstants.C_2_1_AsciiControlCharacters);
        prohibited.addAll(StringPrepConstants.C_2_2_NonAsciiControlCharacters);
        prohibited.addAll(StringPrepConstants.C_3_PrivateUse);
        prohibited.addAll(StringPrepConstants.C_4_NonCharacterCodePoints);
        prohibited.addAll(StringPrepConstants.C_5_SurrogateCodes);
        prohibited.addAll(StringPrepConstants.C_6_InappropriateForPlainText);
        prohibited.addAll(StringPrepConstants.C_7_InappropriateForCanonicalRepresentation);
        prohibited.addAll(StringPrepConstants.C_8_ChangeDisplayPropertiesOrAreDeprecated);
        prohibited.addAll(StringPrepConstants.C_9_TaggingCharacters);
        return prohibited;
    }

    private String codePointAt(String node, int i) {
        int c = node.codePointAt(i);
        return new String(Character.toChars(c));
    }

    private boolean NAND(boolean a, boolean b) {
        return (!(a || b)) || (a ^ b);
    }
}
