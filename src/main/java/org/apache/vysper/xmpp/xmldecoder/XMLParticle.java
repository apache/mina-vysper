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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * holds a particle of XML, either representing an start or end element, or an elements body, or other text nodes.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class XMLParticle {

    private boolean isOpeningElement = false;
    private boolean isClosingElement = false;
    private boolean isSpecialElement = false;

    String elementName = null;
    String content = null;
    public static final String ELEMENT_OPEN = "<";
    public static final String ELEMENT_START_CLOSING = "</";
    public static final String ELEMENT_END_START_AND_END = "/>";
    public static final Pattern PATTERN_NAME_FROM_CLOSINGONLY_ELEMENT = Pattern.compile("\\<[\\!\\?\\/]?\\W*([-:\\w]*)\\W*[\\!\\?\\/]?\\>");
//TODO REMOVE    public static final Pattern PATTERN_NAME_FROM_OPENING_ELEMENT = Pattern.compile("\\<[\\!\\?]?(\\w[\\-\\:\\w]*)\\W+.*");

    public XMLParticle(String content) {
        this.content = content;
        if (content.startsWith(ELEMENT_OPEN)) {
            if (content.startsWith(ELEMENT_START_CLOSING)) {
                isOpeningElement = false;
                isClosingElement = true;
            } else if (content.endsWith(ELEMENT_END_START_AND_END)) {
                isOpeningElement = true;
                isClosingElement = true;
            } else {
                isOpeningElement = true;
                isClosingElement = false;
            }
            if (content.startsWith("<!") && !content.startsWith("<!--")) {
                isSpecialElement = true;
                isOpeningElement = true;
                isClosingElement = true;
            } else if (content.startsWith("<?")) {
                isSpecialElement = true;
                isOpeningElement = true;
                isClosingElement = true;
            }
        }
    }

    public boolean isOpeningElement() {
        return isOpeningElement;
    }

    public boolean isOpeningOnlyElement() {
        return isOpeningElement && !isClosingElement;
    }

    public boolean isClosingElement() {
        return isClosingElement;
    }

    public boolean isClosingOnlyElement() {
        return !isOpeningElement && isClosingElement;
    }

    public boolean isSpecialElement() {
        return isSpecialElement;
    }

    public boolean isText() {
        return !isOpeningElement && !isClosingElement;
    }

    public String getContent() {
        return content;
    }

    public String getContentWithoutElement() throws DecodingException {
        String elementNameLocal = getElementName();
        int i = content.indexOf(elementNameLocal);
        if (i < 0) return null;
        return content.substring(i + elementNameLocal.length());
    }

    public String getElementName() throws DecodingException {
        if (elementName != null) return elementName;
        Matcher matcher = null;
        if (isClosingOnlyElement()) {
            matcher = PATTERN_NAME_FROM_CLOSINGONLY_ELEMENT.matcher(content);
            if (!matcher.matches()) throw new DecodingException("closing element name could not be determined by parser for " + content);
        } else if (isOpeningElement()) {
            elementName = parseElementName();
            if (":".equals(elementName)) throw new DecodingException("':' is not a legitimate XML element name");
            return elementName;
        } else throw new IllegalStateException("element must be opening or closing (or both)");
        elementName = matcher.group(1);
        return elementName;
    }

    public String parseElementName() throws DecodingException {
        StringBuilder elementNameBuilder = new StringBuilder();
        boolean beforeElement = true;
        for (int i = 0; i < content.length(); i++) {
            int current = content.codePointAt(i);
            if (i == 0) {
                if (current != '<') throw new DecodingException("element does not start with '<'");
                continue;
            } else if (i == 1) {
                if (current == '!' || current == '?' || current == '/') continue; // TODO check, if next char is '>'
            }
            if (beforeElement) {
                if (!isLegitemateNameStartChar(current)) {
                    throw new DecodingException("cannot start element name with char " + (char)current);
                }
                beforeElement = false;
            } else {
                if (!isLegitemateNameChar(current)) {
                    if (!isWhitespace(current) &&  current != '>' && current != '/') {
                        throw new DecodingException("char not allowed in element name: " + (char)current);
                    } else {
                        break; // name is completed
                    }
                }
            }
            elementNameBuilder.append((char)current);
        }
        return elementNameBuilder.toString();
    }

    private boolean isWhitespace(int current) {
        return (Character.isWhitespace(current) || /* next char is not relevant */
                current == '!' || current == '?' /* TODO check, if next char is '>'*/
               );
    }

    /**
     *
     * NameChar	 ::=  Letter | Digit | '.' | '-' | '_' | ':' | CombiningChar | Extender
     * @param c
     * @return
     */
    private boolean isLegitemateNameChar(int c) {
        return isLegitemateNameStartChar(c) || Character.isDigit(c) || c == '.' || c == '-';
    }

    private boolean isLegitemateNameStartChar(int c) {
        return Character.isLetter(c) || c == '_' || c == ':';
    }

}
