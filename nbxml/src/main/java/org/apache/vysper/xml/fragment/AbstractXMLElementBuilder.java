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

package org.apache.vysper.xml.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

/**
 * TODO For now, this is mostly a copy of StanzaBuilder. Both classes needs to be refactored.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SuppressWarnings("unchecked")
public abstract class AbstractXMLElementBuilder<B extends AbstractXMLElementBuilder, T extends XMLElement> {

    class ElementStruct {
        public ElementStruct parentElement = null;

        public XMLElement element = null;

        public List<Attribute> attributes = null;

        public Map<String, String> namespaces = null;

        public List<XMLFragment> innerFragments = null;
    }

    /**
     * parent hierarchy for current element
     */
    private Stack<ElementStruct> stack = new Stack<ElementStruct>();

    protected ElementStruct currentElement = null;

    private XMLElement resultingElement = null;

    private boolean isReset = false;

    public AbstractXMLElementBuilder(String elementName) {
        this(elementName, null);
    }

    public AbstractXMLElementBuilder(String elementName, String namespaceURI) {
        this(elementName, namespaceURI, null);
    }

    public AbstractXMLElementBuilder(String elementName, String namespaceURI, String namespacePrefix) {
        startNewElement(elementName, namespaceURI, namespacePrefix);
        resultingElement = currentElement.element;
        stack.push(currentElement);
    }

    public AbstractXMLElementBuilder(String elementName, String namespaceURI, String namespacePrefix,
            List<Attribute> attributes, Map<String, String> namespaces, List<XMLFragment> innerFragments) {
        startNewElement(elementName, namespaceURI, namespacePrefix);
        resultingElement = currentElement.element;
        if (attributes != null)
            currentElement.attributes.addAll(attributes);
        if (namespaces != null)
            currentElement.namespaces.putAll(namespaces);
        if (innerFragments != null)
            currentElement.innerFragments.addAll(innerFragments);
        stack.push(currentElement);
    }

    protected XMLElement createElement(String namespaceURI, String name, String namespacePrefix,
            List<Attribute> attributes, Map<String, String> namespaces, List<XMLFragment> innerFragments) {
        return new XMLElement(namespaceURI, name, namespacePrefix, attributes, innerFragments, namespaces);
    }

    public void startNewElement(String name, String namespaceURI, String namespacePrefix) {
        // TODO assert that name does not contain namespace (":")
        ElementStruct element = new ElementStruct();
        element.attributes = new ArrayList<Attribute>();
        element.namespaces = new HashMap<String, String>();
        
        if(namespacePrefix == null) {
            namespacePrefix = "";
        }
        if(namespaceURI != null && namespacePrefix.length() > 0) {
            element.namespaces.put(namespacePrefix, namespaceURI);
        }
        element.innerFragments = new ArrayList<XMLFragment>();
        element.element = createElement(namespaceURI, name, namespacePrefix, element.attributes, element.namespaces,
                element.innerFragments);

        currentElement = element;
    }

    public B declareNamespace(String namespacePrefix, String value) {
        if(currentElement.namespaces.containsValue(value)) {
            for(Entry<String, String> ns : currentElement.namespaces.entrySet()) {
                if(ns.getValue().equals(value)) {
                    currentElement.namespaces.remove(ns.getKey());
                }
            }
        }
        
        currentElement.namespaces.put(namespacePrefix, value);
        return (B) this;
    }

    public B addAttribute(String name, String value) {
        addAttribute(new Attribute(name, value));
        return (B) this;
    }

    public B addAttribute(String namespaceUris, String name, String value) {
        addAttribute(new Attribute(namespaceUris, name, value));
        return (B) this;
    }

    public B addAttribute(Attribute attribute) {
        checkReset();
        // check if attribute already exists
        for(Attribute existing : currentElement.attributes) {
            if(existing.getName().equals(attribute.getName())
                    && existing.getNamespaceUri().equals(attribute.getNamespaceUri())) {
                currentElement.attributes.remove(existing);
                break;
            }
        }
        
        currentElement.attributes.add(attribute);
        return (B) this;
    }

    public B addText(String text) {
        checkReset();
        currentElement.innerFragments.add(new XMLText(text));
        return (B) this;
    }

    public B startInnerElement(String name) {
        return this.startInnerElement(name, null);
    }

    public B startInnerElement(String name, String namespaceURI) {
        checkReset();

        startNewElement(name, namespaceURI, null);

        stack.peek().innerFragments.add(currentElement.element); // add new one to its parent

        stack.push(currentElement);

        return (B) this;
    }

    public B endInnerElement() {
        checkReset();
        if (stack.isEmpty())
            throw new IllegalStateException("cannot end beyond top element");

        stack.pop(); // take current off stack and forget (it was added to its parent before)
        currentElement = stack.peek(); // we again deal with parent, which can be receive additions
        return (B) this;
    }

    public B addPreparedElement(XMLElement preparedElement) {
        checkReset();
        currentElement.innerFragments.add(preparedElement);
        return (B) this;
    }

    /**
     * the stanza can only be retrieved once
     * @return retrieves the XML element and invalidates the builder
     */
    public T build() {
        checkReset();
        XMLElement returnStanza = resultingElement;
        resultingElement = null;
        isReset = true; // reset
        stack.clear();
        return (T) returnStanza;
    }

    /**
     * assure that the immutable XML element object is not changed after it was retrieved
     */
    private void checkReset() {
        if (isReset)
            throw new IllegalStateException("XML element builder was reset after retrieving stanza");
    }
}
