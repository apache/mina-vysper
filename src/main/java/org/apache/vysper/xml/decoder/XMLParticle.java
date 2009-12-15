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
package org.apache.vysper.xml.decoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * holds a particle of XML, either representing an start or end element, or an elements body, or other text nodes.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLParticle {

	//private static final String nameStartChar = "[:A-Z_a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
	
	// TODO how do we handle \\U00010000-\\U000EFFFF ?
    private static final String nameStartChar = ":A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD";
    private static final String nameChar = nameStartChar + "-\\.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040";
    public static final Pattern NAME_PATTERN = Pattern.compile("^[" + nameStartChar + "][" + nameChar + "]*$");
    public static final Pattern NAME_PREFIX_PATTERN = Pattern.compile("^xml", Pattern.CASE_INSENSITIVE);
    
    public static final Pattern UNESCAPE_UNICODE_PATTERN = Pattern.compile("\\&\\#(x?)(.+);");
	
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
    	if(isText()) {
    		return unescape(content);
    	} else {
    		return content;
    	}
    }
    
    
    private String unescape(String s) {
    	s = s.replace("&amp;", "&").replace("&gt;", ">").replace("&lt;", "<").replace("&apos;", "'").replace("&quot;", "\"");
    
    	StringBuffer sb = new StringBuffer();

    	Matcher matcher = UNESCAPE_UNICODE_PATTERN.matcher(s);
    	int end = 0;
    	while(matcher.find()) {
    		boolean isHex = matcher.group(1).equals("x");
    		String unicodeCode = matcher.group(2);
    		
    		int base = isHex ? 16: 10;
    		int i = Integer.valueOf(unicodeCode, base).intValue();
    		char[] c = Character.toChars(i);
    		sb.append(s.substring(end, matcher.start()));
    		end = matcher.end();
    		sb.append(c);
    	}
    	sb.append(s.substring(end, s.length()));
    	
    	return sb.toString();
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
                beforeElement = false;
            } else {
                if (isWhitespace(current) || current == '>' || current == '/') {
                    break; // name is completed
                }
            }
            elementNameBuilder.append((char)current);
        }
        
        String elementName = elementNameBuilder.toString();
        
        if(!NAME_PATTERN.matcher(elementName).find()) throw new DecodingException("Invalid element name: " + elementName);

        // element names must not begin with "xml" in any casing
        if(NAME_PREFIX_PATTERN.matcher(elementName).find()) throw new DecodingException("Names must not start with 'xml': " + elementName);
        
        return elementName;
    }

    private boolean isWhitespace(int current) {
        return (Character.isWhitespace(current) || /* next char is not relevant */
                current == '!' || current == '?' /* TODO check, if next char is '>'*/
               );
    }
}
