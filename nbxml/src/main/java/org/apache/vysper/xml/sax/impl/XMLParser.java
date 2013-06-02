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
package org.apache.vysper.xml.sax.impl;

import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.xml.sax.impl.XMLTokenizer.TokenListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLParser implements TokenListener {

    private Logger log = LoggerFactory.getLogger(XMLParser.class);

    private static final String nameStartChar = ":A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD";

    private static final String nameChar = nameStartChar + "-\\.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040";

    public static final Pattern NAME_PATTERN = Pattern.compile("^[" + nameStartChar + "][" + nameChar + "]*$");

    public static final Pattern NAME_PREFIX_PATTERN = Pattern.compile("^xml", Pattern.CASE_INSENSITIVE);

    public static final Pattern UNESCAPE_UNICODE_PATTERN = Pattern.compile("\\&\\#(x?)([0-9a-fA-F]++);");

    private ContentHandler contentHandler;

    private ErrorHandler errorHandler;

    private ParserNamespaceResolver nsResolver = new ParserNamespaceResolver();

    private static enum State {
        START, IN_TAG, IN_DECLARATION, IN_END_TAG, AFTER_START_NAME, AFTER_END_NAME, IN_EMPTY_TAG, AFTER_ATTRIBUTE_NAME, AFTER_ATTRIBUTE_EQUALS, AFTER_ATTRIBUTE_FIRST_QUOTE, AFTER_ATTRIBUTE_VALUE, AFTER_COMMENT_BANG, AFTER_COMMENT_DASH1, AFTER_COMMENT_DASH2, AFTER_COMMENT, AFTER_COMMENT_CLOSING_DASH1, AFTER_COMMENT_CLOSING_DASH2, AFTER_COMMENT_ENDING_DASH1, AFTER_COMMENT_ENDING_DASH2, CLOSED
    }

    private XMLTokenizer tokenizer;

    private State state = State.START;

    private String qname;

    // qname/value map
    private Map<String, String> attributes;

    private String attributeName;

    // element names as {uri}qname
    private Stack<String> elements = new Stack<String>();

    private boolean sentStartDocument = false;

    // features
    private boolean reportNsAttributes = false;

    private boolean commentsAllowed = true;

    private boolean restartsAllowed = false;

    private String restartQname = null;

    public XMLParser(ContentHandler contentHandler, ErrorHandler errorHandler, Map<String, Boolean> features,
            Map<String, Object> properties) {
        this.contentHandler = contentHandler;
        this.errorHandler = errorHandler;

        commentsAllowed = feature(features, DefaultNonBlockingXMLReader.FEATURE_COMMENTS_ALLOWED, true);
        reportNsAttributes = feature(features, DefaultNonBlockingXMLReader.FEATURE_NAMESPACE_PREFIXES, false);
        reportNsAttributes = feature(features, DefaultNonBlockingXMLReader.FEATURE_NAMESPACE_PREFIXES, false);
        restartsAllowed = feature(features, DefaultNonBlockingXMLReader.FEATURE_RESTART_ALLOWED, false);
        restartQname = (String) properties.get(DefaultNonBlockingXMLReader.PROPERTY_RESTART_QNAME);

        this.tokenizer = new XMLTokenizer(this);
    }

    private boolean feature(Map<String, Boolean> features, String name, boolean defaultValue) {
        if (features.containsKey(name)) {
            return features.get(name);
        } else {
            return defaultValue;
        }
    }

    public void parse(IoBuffer byteBuffer, CharsetDecoder charsetDecoder) throws SAXException {
        if (state == State.CLOSED)
            throw new SAXException("Parser is closed");

        try {
            tokenizer.parse(byteBuffer, charsetDecoder);
        } catch (RuntimeException e) {
            e.printStackTrace();
            fatalError(e.getMessage());
        }
    }

    public void token(char c, String token) throws SAXException {
        if (log.isTraceEnabled()) {
            String s = (token == null) ? Character.toString(c) : token;
            log.trace("Parser got token {} in state {}", s, state);
        }

        switch (state) {
        case START:
            if (c == '<') {
                state = State.IN_TAG;
                attributes = new HashMap<String, String>();
            } else {
                characters(token);
            }
            break;
        case IN_TAG:
            // token must be element name or / for a end tag
            if (c == '/') {
                state = State.IN_END_TAG;
            } else if (c == '?') {
                state = State.IN_DECLARATION;
                xmlDeclaration();
            } else if (c == '!') {
                if (commentsAllowed) {
                    state = State.AFTER_COMMENT_BANG;
                } else {
                    fatalError("Comments not allowed");
                    return;
                }
            } else {
                if (token != null && isValidName(token)) {
                    qname = token;
                    state = State.AFTER_START_NAME;
                } else {
                    if(token != null) {
                        fatalError("Invalid element name: " + qname);
                    } else {
                        fatalError("Not well-formed start tag");
                    }
                    return;
                }
            }
            break;
        case IN_END_TAG:
            // token must be element name
            qname = token;
            state = State.AFTER_END_NAME;
            break;
        case AFTER_START_NAME:
            // token must be attribute name or > or /
            if (c == '>') {
                // end of start or end tag
                if (state == State.AFTER_START_NAME) {
                    startElement();
                    state = State.START;
                    attributes = null;
                } else if (state == State.AFTER_END_NAME) {
                    state = State.START;
                    endElement();
                }
            } else if (c == '/') {
                state = State.IN_EMPTY_TAG;
            } else {
                // must be attribute name
                attributeName = token;
                state = State.AFTER_ATTRIBUTE_NAME;
            }
            break;
        case AFTER_ATTRIBUTE_NAME:
            // token must be =
            if (c == '=') {
                state = State.AFTER_ATTRIBUTE_EQUALS;
            } else {
                fatalError("Not wellformed");
            }
            break;
        case AFTER_ATTRIBUTE_EQUALS:
            // token must be " or '
            if (c == '"' || c == '\'') {
                state = State.AFTER_ATTRIBUTE_FIRST_QUOTE;
            }
            break;
        case AFTER_ATTRIBUTE_FIRST_QUOTE:
            // token must be attribute value
            attributes.put(attributeName, unescape(token));
            state = State.AFTER_ATTRIBUTE_VALUE;
            break;
        case AFTER_ATTRIBUTE_VALUE:
            // token must be " or '
            if (c == '"' || c == '\'') {
                state = State.AFTER_START_NAME;
            } else {
                fatalError("Not wellformed");
            }
            break;
        case AFTER_END_NAME:
            // token must be >
            if (c == '>') {
                state = State.START;
                endElement();
            }
            break;
        case IN_EMPTY_TAG:
            // token must be >
            if (c == '>') {
                startElement();
                attributes = null;

                if (state != State.CLOSED) {
                    state = State.START;
                    endElement();
                }
            }
            break;
        case AFTER_COMMENT_BANG:
            // token must be -
            if (c == '-') {
                state = State.AFTER_COMMENT_DASH1;
            } else {
                fatalError("Comment not wellformed");
                return;
            }
            break;
        case AFTER_COMMENT_DASH1:
            // token must be -
            if (c == '-') {
                state = State.AFTER_COMMENT_DASH2;
            } else {
                fatalError("Comment not wellformed");
                return;
            }
            break;
        case AFTER_COMMENT_DASH2:
            // we should now get the comment content, ignore
            if (c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH1;
            } else {
                state = State.AFTER_COMMENT;
            }
            break;
        case AFTER_COMMENT:
            // token must be - or some text
            if (c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH1;
            } else if (c == '>') {
                fatalError("Comment not wellformed");
                return;
            } else {
                // ignore
            }
            break;
        case AFTER_COMMENT_CLOSING_DASH1:
            // token must be -
            if (c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH2;
            } else {
                fatalError("Comment not wellformed");
                return;
            }
            break;
        case AFTER_COMMENT_CLOSING_DASH2:
            // token must be >
            if (c == '>') {
                state = State.START;
            } else {
                fatalError("Comment not wellformed");
                return;
            }
            break;
        case IN_DECLARATION:
            // wait for >
            if (c == '>') {
                state = State.START;
            }
            break;
        }
    }

    private void characters(String s) throws SAXException {
        // text only allowed in element
        if (!elements.isEmpty()) {
            String unescaped = unescape(s);
            log.trace("Parser emitting characters \"{}\"", unescaped);
            contentHandler.characters(unescaped.toCharArray(), 0, unescaped.length());
        } else if (s.trim().length() > 0) {
            // must start document, even that document is not wellformed
            startDocument();
            fatalError("Text only allowed in element");
        } else {
            // ignorable whitespace
            startDocument();
        }
    }

    private boolean isValidName(String name) {
        // element names must only contain valid characters
        // element names must not begin with "xml" in any casing
        return NAME_PATTERN.matcher(name).find() && !NAME_PREFIX_PATTERN.matcher(name).find();
    }

    private boolean needsRestart() {
        return elements.size() > 0;
    }

    private void restart() {
        log.trace("Restarting XML stream");

        elements.clear();
        nsResolver = new ParserNamespaceResolver();
        sentStartDocument = false;
        tokenizer.restart();
    }

    private void xmlDeclaration() {
        // we got an XML declaration, should we restart stream?
        // TODO could also be a PI, if we want to support PIs, this code needs further attention
        if (needsRestart()) {
            if (restartsAllowed) {
                // ok, restart
                restart();
            } else {
                // restarts not allowed, fail 
            }
        }
    }

    private void startDocument() throws SAXException {
        if (!sentStartDocument) {
            contentHandler.startDocument();
            sentStartDocument = true;
        }
    }

    private void startElement() throws SAXException {
        log.trace("StartElement {}", qname);

        // check if this should restart stream
        if (restartsAllowed && needsRestart() && qname.equals(restartQname)) {
            restart();
        }

        if (elements.isEmpty()) {
            startDocument();
        }

        // find all namespace declarations so we can populate the NS resolver
        Map<String, String> nsDeclarations = new HashMap<String, String>();
        for (Entry<String, String> attribute : attributes.entrySet()) {
            if (attribute.getKey().equals("xmlns")) {
                // is namespace attribute
                nsDeclarations.put("", attribute.getValue());
            } else if (attribute.getKey().startsWith("xmlns:")) {
                nsDeclarations.put(attribute.getKey().substring(6), attribute.getValue());
            }
        }
        nsResolver.push(nsDeclarations);

        // find all non-namespace attributes
        List<Attribute> nonNsAttributes = new ArrayList<Attribute>();
        for (Entry<String, String> attribute : attributes.entrySet()) {
            String attQname = attribute.getKey();

            // only report NS declaration attributes if the feature is set to
            if (reportNsAttributes) {
                nonNsAttributes.add(new Attribute(attQname, null, attQname, attribute.getValue()));
            } else if (!attQname.equals("xmlns") && !attQname.startsWith("xmlns:")) {
                String attLocalName = extractLocalName(attQname);
                String attPrefix = extractNsPrefix(attQname);
                String attUri;
                if (attPrefix.length() > 0) {
                    attUri = nsResolver.resolveUri(attPrefix);
                    if (attUri == null) {
                        if (attPrefix.length() > 0) {
                            fatalError("Undeclared namespace prefix: " + attPrefix);
                            return;
                        } else {
                            attUri = "";
                        }
                    }
                } else {
                    // by default, attributes are in the empty namespace
                    attUri = "";
                }
                nonNsAttributes.add(new Attribute(attLocalName, attUri, attQname, attribute.getValue()));
            }
        }

        String prefix = extractNsPrefix(qname);
        String uri = nsResolver.resolveUri(prefix);
        if (uri == null) {
            if (prefix.length() > 0) {
                fatalError("Undeclared namespace prefix: " + prefix);
                return;
            } else {
                uri = "";
            }
        }

        String localName = extractLocalName(qname);

        elements.add(fullyQualifiedName(uri, qname));

        contentHandler.startElement(uri, localName, qname, new DefaultAttributes(nonNsAttributes));
    }

    private String extractLocalName(String qname) {
        int index = qname.indexOf(':');

        if (index > -1) {
            return qname.substring(index + 1);
        } else {
            return qname;
        }
    }

    private String extractNsPrefix(String qname) {
        int index = qname.indexOf(':');

        if (index > -1) {
            return qname.substring(0, index);
        } else {
            return "";
        }
    }

    private String fullyQualifiedName(String uri, String qname) {
        return "{" + uri + "}" + qname;
    }

    private void endElement() throws SAXException {
        log.trace("EndElement {}", qname);

        if (state == State.CLOSED)
            return;

        String prefix = extractNsPrefix(qname);
        String uri = nsResolver.resolveUri(prefix);
        if (uri == null) {
            if (prefix.length() > 0) {
                fatalError("Undeclared namespace prefix: " + prefix);
                return;
            } else {
                uri = "";
            }
        }

        nsResolver.pop();

        String localName = extractLocalName(qname);

        String fqn = elements.pop();
        if (fqn.equals(fullyQualifiedName(uri, qname))) {
            contentHandler.endElement(uri, localName, qname);

            if (elements.isEmpty()) {
                contentHandler.endDocument();
                state = State.CLOSED;
            }
        } else {
            fatalError("Invalid element name " + qname);
        }
    }

    private void fatalError(String message) throws SAXException {
        log.debug("Fatal error: {}", message);
        state = State.CLOSED;
        tokenizer.close();

        // make sure we send a start document event
        startDocument();

        errorHandler.fatalError(new SAXParseException(message, null));
    }

    private String unescape(String s) {
        s = s.replace("&amp;", "&").replace("&gt;", ">").replace("&lt;", "<").replace("&apos;", "'").replace("&quot;",
                "\"");

        StringBuffer sb = new StringBuffer();

        Matcher matcher = UNESCAPE_UNICODE_PATTERN.matcher(s);
        int end = 0;
        while (matcher.find()) {
            boolean isHex = matcher.group(1).equals("x");
            String unicodeCode = matcher.group(2);

            int base = isHex ? 16 : 10;
            int i = Integer.valueOf(unicodeCode, base).intValue();
            char[] c = Character.toChars(i);
            sb.append(s.substring(end, matcher.start()));
            end = matcher.end();
            sb.append(c);
        }
        sb.append(s.substring(end, s.length()));

        return sb.toString();
    }
}
