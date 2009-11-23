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

package org.apache.vysper.xmpp.xmlfragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * TODO For now, this is mostly a copy of StanzaBuilder. Both classes needs to be refactored.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLElementBuilder {

    class ElementStruct {
        public ElementStruct parentElement = null;
        public XMLElement element = null;
        public List<Attribute> attributes = null;
        public List<XMLFragment> innerFragments = null;
    }

    /**
     * parent hierarchy for current element
     */
    private Stack<ElementStruct> stack = new Stack<ElementStruct>();
    private ElementStruct currentElement = null;
    private XMLElement resultingElement = null;
    private boolean isReset = false;


    public XMLElementBuilder(String elementName) {
        this(elementName, null);
    }

    public XMLElementBuilder(String elementName, String namespaceURI) {
        this(elementName, namespaceURI, null);
    }

    public XMLElementBuilder(String elementName, String namespaceURI, String namespacePrefix) {
        startNewElement(elementName, namespaceURI, namespacePrefix);
        resultingElement = currentElement.element;
        stack.push(currentElement);
    }
    
    private void startNewElement(String name, String namespaceURI, String namespacePrefix) {
        // TODO assert that name does not contain namespace (":")
        // TODO handle the namespace, given by URI, currently always NULL in XMLElement constructors
        ElementStruct element = new ElementStruct();
        element.attributes = new ArrayList<Attribute>();
        element.innerFragments = new ArrayList<XMLFragment>();
        element.element = new XMLElement(namespaceURI, name, namespacePrefix, element.attributes, element.innerFragments);

        currentElement = element;
    }


    public XMLElementBuilder addNamespaceAttribute(String value) {
        addAttribute(new NamespaceAttribute(value));
        return this;
    }

    public XMLElementBuilder addNamespaceAttribute(String namespacePrefix, String value) {
        addAttribute(new NamespaceAttribute(namespacePrefix, value));
        return this;
    }

    public XMLElementBuilder addAttribute(String name, String value) {
    	addAttribute(new Attribute(name, value));
    	return this;
    }

    public XMLElementBuilder addAttribute(String namespaceUris, String name, String value) {
    	addAttribute(new Attribute(namespaceUris, name, value));
    	return this;
    }

    
    public XMLElementBuilder addAttribute(Attribute attribute) {
        checkReset();
        currentElement.attributes.add(attribute);
        return this;
    }

    public XMLElementBuilder addText(String text) {
        checkReset();
        currentElement.innerFragments.add(new XMLText(text));
        return this;
    }

    public XMLElementBuilder startInnerElement(String name) {
        return this.startInnerElement(name, null);
    }

    public XMLElementBuilder startInnerElement(String name, String namespaceURI) {
        checkReset();

        startNewElement(name, namespaceURI, null);

        stack.peek().innerFragments.add(currentElement.element); // add new one to its parent

        stack.push(currentElement);

        return this;
    }

    public XMLElementBuilder endInnerElement() {
        checkReset();
        if (stack.isEmpty()) throw new IllegalStateException("cannot end beyond top element");

        stack.pop(); // take current off stack and forget (it was added to its parent before)
        currentElement = stack.peek(); // we again deal with parent, which can be receive additions
        return this;
    }

    public XMLElementBuilder addPreparedElement(XMLElement preparedElement) {
        checkReset();
        currentElement.innerFragments.add(preparedElement);
        return this;
    }

    /**
     * the stanza can only be retrieved once
     * @return retrieves the XML element and invalidates the builder
     */
    public XMLElement getFinalElement() {
        checkReset();
        XMLElement returnStanza = resultingElement;
        resultingElement = null;
        isReset = true; // reset
        stack.clear();
        return returnStanza;
    }

    /**
     * assure that the immutable XML element object is not changed after it was retrieved
     */
    private void checkReset() {
        if (isReset) throw new IllegalStateException("XML element builder was reset after retrieving stanza");
    }
}
