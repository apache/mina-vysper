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
package org.apache.vysper.xmpp.modules.extension.xep0059_result_set_management;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * The Set element as defined by
 * <a href="https://xmpp.org/extensions/xep-0059.html#schema">XEP-0059 Result
 * Set Management</a>
 * 
 * @author Réda Housni Alaoui
 */
public class Set {

    public static final String ELEMENT_NAME = "set";

    private static final String FIRST = "first";

    private static final String INDEX = "index";

    private final XMLElement element;

    public Set(XMLElement element) {
        if (!ELEMENT_NAME.equals(element.getName())) {
            throw new IllegalArgumentException("ResultSetManagement element must be named '" + ELEMENT_NAME
                    + "' instead of '" + element.getName() + "'");
        }
        if (!NamespaceURIs.XEP0059_RESULT_SET_MANAGEMENT.equals(element.getNamespaceURI())) {
            throw new IllegalArgumentException("ResultSetManagement element must be bound to namespace uri '"
                    + NamespaceURIs.XEP0059_RESULT_SET_MANAGEMENT + "' instead of '" + element.getNamespaceURI() + "'");
        }
        this.element = element;
    }

    public static Set empty() {
        return new Set(new XMLElement(NamespaceURIs.XEP0059_RESULT_SET_MANAGEMENT, ELEMENT_NAME, null,
                Collections.emptyList(), Collections.emptyList()));
    }

    public XMLElement element() {
        return element;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getAfter() throws XMLSemanticError {
        return getElementText("after");
    }

    public Optional<String> getBefore() throws XMLSemanticError {
        return getElementText("before");
    }

    public Optional<Long> getCount() throws XMLSemanticError {
        return getElementLong("count");
    }

    public Optional<First> getFirst() throws XMLSemanticError {
        return ofNullable(element.getSingleInnerElementsNamed(FIRST)).map(First::new);
    }

    public Optional<Long> getIndex() throws XMLSemanticError {
        return getElementLong(INDEX);
    }

    public Optional<String> getLast() throws XMLSemanticError {
        return getElementText("last");
    }

    public Optional<Long> getMax() throws XMLSemanticError {
        return getElementLong("max");
    }

    private Optional<String> getElementText(String elementName) throws XMLSemanticError {
        XMLElement xmlElement = element.getSingleInnerElementsNamed(elementName);
        if (xmlElement == null) {
            return Optional.empty();
        }

        XMLText xmlText = xmlElement.getInnerText();
        if (xmlText == null) {
            return Optional.of("");
        }

        String text = xmlText.getText();
        if (text == null) {
            return Optional.of("");
        }

        return Optional.of(text);
    }

    private Optional<Long> getElementLong(String elementName) throws XMLSemanticError {
        return getElementText(elementName).map(Long::parseLong);
    }

    public static class First {

        private final XMLElement element;

        private First(XMLElement element) {
            this.element = element;
        }

        private XMLElement element() {
            return element;
        }

        public String getValue() {
            return element.getInnerText().getText();
        }

        public Optional<Long> getIndex() {
            return ofNullable(element.getAttributeValue(INDEX)).map(Long::parseLong);
        }

    }

    public static class Builder {

        private String after;

        private String before;

        private Long count;

        private First first;

        private Long index;

        private String last;

        private Long max;

        private Builder() {

        }

        public Builder after(String after) {
            this.after = after;
            return this;
        }

        public Builder before(String before) {
            this.before = before;
            return this;
        }

        public Builder count(Long count) {
            this.count = count;
            return this;
        }

        public FirstBuilder startFirst() {
            return new FirstBuilder(this);
        }

        public Builder index(Long index) {
            this.index = index;
            return this;
        }

        public Builder last(String last) {
            this.last = last;
            return this;
        }

        public Builder max(Long max) {
            this.max = max;
            return this;
        }

        private XMLFragment createElement(String name, Object innerValue) {
            return new XMLElement(null, name, null, Collections.emptyList(),
                    Collections.singletonList(new XMLText(String.valueOf(innerValue))));
        }

        public Set build() {
            List<XMLFragment> innerFragments = new ArrayList<>();
            ofNullable(after).map(innerValue -> createElement("after", innerValue)).ifPresent(innerFragments::add);
            ofNullable(before).map(innerValue -> createElement("before", innerValue)).ifPresent(innerFragments::add);
            ofNullable(count).map(innerValue -> createElement("count", innerValue)).ifPresent(innerFragments::add);
            ofNullable(first).map(First::element).ifPresent(innerFragments::add);
            ofNullable(index).map(innerValue -> createElement("index", innerValue)).ifPresent(innerFragments::add);
            ofNullable(last).map(innerValue -> createElement("last", innerValue)).ifPresent(innerFragments::add);
            ofNullable(max).map(innerValue -> createElement("max", innerValue)).ifPresent(innerFragments::add);

            return new Set(new XMLElement(NamespaceURIs.XEP0059_RESULT_SET_MANAGEMENT, ELEMENT_NAME, null,
                    Collections.emptyList(), innerFragments));
        }

    }

    public static class FirstBuilder {

        private final Builder builder;

        private String value;

        private Long index;

        private FirstBuilder(Builder builder) {
            this.builder = requireNonNull(builder);
        }

        public FirstBuilder value(String value) {
            this.value = value;
            return this;
        }

        public FirstBuilder index(Long index) {
            this.index = index;
            return this;
        }

        public Builder endFirst() {
            List<Attribute> attributes = new ArrayList<>();
            ofNullable(index).map(value -> new Attribute(INDEX, String.valueOf(index))).ifPresent(attributes::add);
            List<XMLFragment> innerFragments = new ArrayList<>();
            ofNullable(value).map(XMLText::new).ifPresent(innerFragments::add);

            builder.first = new First(new XMLElement(null, FIRST, null, attributes, innerFragments));
            return builder;
        }

    }

}
