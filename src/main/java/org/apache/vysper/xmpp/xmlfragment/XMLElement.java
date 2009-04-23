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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * an immutable xml element specialized for XMPP.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class XMLElement implements XMLFragment {

    private String name;

    /**
     * example element: <a:b xmlns:a="http://ns.org" >
     * element's namespace. the prefix for the element name is taken from the corresponding attribute.
     * if the namespace is 'http://ns.org', then element name b is prefixed with a.
     * NOTE: the namespace value must NOT be "b"!
     */
    private String namespace;

    private List<Attribute> attributes;
    private List<XMLFragment> innerFragments;
    protected XMLElementVerifier xmlElementVerifier;

    public XMLElement(String name, String namespace, Attribute[] attributes, XMLFragment[] innerFragments) {
        this(name, namespace, FragmentFactory.asList(attributes), FragmentFactory.asList(innerFragments));
    }

     public XMLElement(String name, String namespace, List<Attribute> attributes, XMLFragment[] innerFragments) {
        this(name, namespace, attributes, FragmentFactory.asList(innerFragments));
    }

     public XMLElement(String name, String namespace, Attribute[] attributes, List<XMLFragment> innerFragments) {
        this(name, namespace, FragmentFactory.asList(attributes), innerFragments);
    }

    public XMLElement(String name, String namespace, List<Attribute> attributes, List<XMLFragment> innerFragments) {
        this.namespace = namespace == null ? NamespaceAttribute.DEFAULT_NAMESPACE : namespace;
        this.name = name;
        this.attributes = (attributes == null) ? Collections.EMPTY_LIST : Collections.unmodifiableList(attributes);
        this.innerFragments = (innerFragments == null) ? Collections.EMPTY_LIST : Collections.unmodifiableList(innerFragments);
        if (name == null) throw new IllegalArgumentException("XMLElement name cannot be null");
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public Attribute getAttribute(String name) {
        for (Attribute attribute : attributes) {
            if (attribute.getName().equals(name)) return attribute;
        }
        return null;
    }

    public String getAttributeValue(String name) {
        Attribute attribute = getAttribute(name);
        if (attribute == null) return null;
        else return attribute.getValue();
    }

    public String getXMLLang() {
        return getAttributeValue("xml:lang");
    }

    public List<XMLFragment> getInnerFragments() {
        return innerFragments;
    }

    public XMLElement getFirstInnerElement() {
        if (innerFragments == null || innerFragments.size() < 1) return null;
        for (XMLFragment xmlFragment : innerFragments) {
            if (xmlFragment instanceof XMLElement) return (XMLElement)xmlFragment;
        }
        return null;
    }

    public List<XMLElement> getInnerElements() {
        List<XMLElement> innerElements = new ArrayList<XMLElement>();
        if (innerFragments == null || innerFragments.size() < 1) return null;
        for (XMLFragment xmlFragment : innerFragments) {
            if (xmlFragment instanceof XMLElement) innerElements.add((XMLElement) xmlFragment);
        }
        return innerElements;
    }

    public List<XMLText> getInnerTexts() {
        List<XMLText> innerTexts = new ArrayList<XMLText>();
        if (innerFragments == null || innerFragments.size() < 1) return null;
        for (XMLFragment xmlFragment : innerFragments) {
            if (xmlFragment instanceof XMLText) innerTexts.add((XMLText) xmlFragment);
        }
        return innerTexts;
    }

    public XMLText getFirstInnerText() {
        if (innerFragments == null || innerFragments.size() < 1) return null;
        for (XMLFragment xmlFragment : innerFragments) {
            if (xmlFragment instanceof XMLText) return (XMLText) xmlFragment;
        }
        return null;
    }

    public XMLText getSingleInnerText() throws XMLSemanticError {
        List<XMLText> innerTexts = getInnerTexts();
        if (innerTexts == null || innerTexts.isEmpty()) return null;
        if (innerTexts.size() > 1) throw new XMLSemanticError("element has more than one inner text fragment");
        return innerTexts.get(0);
    }

    /**
     * collects all inner elements named as given parameter
     * @param name - must not be NULL
     */
    public List<XMLElement> getInnerElementsNamed(String name) {
        if (name == null) return null;
        List<XMLElement> innerElements = getInnerElements();
        if (innerElements == null) return null;
        Iterator<XMLElement> elementIterator = innerElements.iterator(); // this List will be modified now!
        while (elementIterator.hasNext()) {
            XMLElement xmlElement =  elementIterator.next();
            if (!name.equals(xmlElement.getName())) elementIterator.remove();
        }
        return innerElements;
    }

    public XMLElement getSingleInnerElementsNamed(String name) throws XMLSemanticError {
        List<XMLElement> innerElements = getInnerElementsNamed(name);
        if (innerElements == null) return null;
        if (innerElements.isEmpty()) return null;
        if (innerElements.size() > 1) throw new XMLSemanticError("element has more than one inner element named: " + name);
        return innerElements.get(0);
    }

    /**
     * collects all inner elements with given name and puts them in a map indexed by
     * @param name
     * @return Map<String language, XMLElement>
     * @exception no language attribute may occur more than once for the same element
     */
    public Map<String, XMLElement> getInnerElementsByXMLLangNamed(String name) throws XMLSemanticError {
        if (name == null) return null;

        List<XMLElement> innerElements = getInnerElementsNamed(name);
        Map<String, XMLElement> langMap = new HashMap<String, XMLElement>();

        Iterator<XMLElement> elementIterator = innerElements.iterator(); // this List will be modified now!
        while (elementIterator.hasNext()) {
            XMLElement xmlElement =  elementIterator.next();
            String xmlLang = xmlElement.getXMLLang();
            if (langMap.containsKey(xmlLang)) {
                throw new XMLSemanticError("two inner elements '" + name + "' with same language attribute " + xmlLang);
            }
            langMap.put(xmlLang, xmlElement);
        }
        return langMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof XMLElement)) return false;

        final XMLElement that = (XMLElement) o;

        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) return false;
        if (innerFragments != null ? !innerFragments.equals(that.innerFragments) : that.innerFragments != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 29 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 29 * result + (innerFragments != null ? innerFragments.hashCode() : 0);
        return result;
    }

    public XMLElementVerifier getVerifier() {
        if (xmlElementVerifier == null) xmlElementVerifier = new XMLElementVerifier(this);
        return xmlElementVerifier;
    }
}
