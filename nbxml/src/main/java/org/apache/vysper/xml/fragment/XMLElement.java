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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * an immutable xml element specialized for XMPP.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLElement implements XMLFragment {

    private String name;

    /**
     * example element: <a:b xmlns:a="http://ns.org" >
     * element's namespace. the prefix for the element name is taken from the corresponding attribute.
     * if the namespace is 'http://ns.org', then element name b is prefixed with a.
     * NOTE: the namespace value must NOT be "b"!
     */
    private String namespaceURI;

    private String namespacePrefix;

    private List<Attribute> attributes;

    private Map<String, String> namespaces;

    private List<XMLFragment> innerFragments;

    protected XMLElementVerifier xmlElementVerifier;

    public XMLElement(String namespaceURI, String name, String namespacePrefix, Attribute[] attributes,
            XMLFragment[] innerFragments) {
        this(namespaceURI, name, namespacePrefix, attributes, innerFragments, null);
    }

    public XMLElement(String namespaceURI, String name, String namespacePrefix, Attribute[] attributes,
            XMLFragment[] innerFragments, Map<String, String> namespaces) {
        this(namespaceURI, name, namespacePrefix, FragmentFactory.asList(attributes), FragmentFactory
                .asList(innerFragments), namespaces);
    }

    public XMLElement(String namespaceURI, String name, String namespacePrefix, List<Attribute> attributes,
            List<XMLFragment> innerFragments) {
        this(namespaceURI, name, namespacePrefix, attributes, innerFragments, null);
    }

    public XMLElement(String namespaceURI, String name, String namespacePrefix, List<Attribute> attributes,
            List<XMLFragment> innerFragments, Map<String, String> namespaces) {
        this.namespaceURI = namespaceURI == null ? Namespaces.DEFAULT_NAMESPACE_URI : namespaceURI;

        if(namespacePrefix != null && namespacePrefix.length() > 0) {
            if(!isValidName(namespacePrefix) || namespacePrefix.contains(":")) throw new IllegalArgumentException("Invalid XML element namespace prefix");
        }
        this.namespacePrefix = namespacePrefix == null ? Namespaces.DEFAULT_NAMESPACE_PREFIX : namespacePrefix;
        
        if(name == null || !isValidName(name)) throw new IllegalArgumentException("Invalid XML element name");
        this.name = name;
        this.attributes = (attributes == null) ? Collections.EMPTY_LIST : Collections.unmodifiableList(attributes);
        this.namespaces = (namespaces == null) ? Collections.EMPTY_MAP : Collections.unmodifiableMap(namespaces);
        this.innerFragments = (innerFragments == null) ? Collections.EMPTY_LIST : Collections
                .unmodifiableList(innerFragments);
    }
    
    private static final String NAME_START_CHAR = "A-Za-z\\_\\:";
    private static final String NAME_CHAR = NAME_START_CHAR + "\\-\\.0-9";
    private static final Pattern namePattern = Pattern.compile("[" + NAME_START_CHAR + "][" + NAME_CHAR + "]*");
    private boolean isValidName(String name) {
        // TODO add additional char ranges
        return namePattern.matcher(name).matches();
    }

    public String getName() {
        return name;
    }

    /**
     * Return the XML namespace prefix. 
     * @return The namespace prefix. If the element does not have a prefix 
     *  , thus being part of the default namespace, this method will return
     *  an empty string. 
     */
    public String getNamespacePrefix() {
        return namespacePrefix;
    }

    /**
     * Return the namespace URI.
     * @return The namespace URI for the element. 
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public Attribute getAttribute(String name) {
        return getAttribute("", name);
    }

    public Attribute getAttribute(String namespaceUri, String name) {
        for (Attribute attribute : attributes) {
            // name must match and must be in empty namespace
            if (attribute.getName().equals(name) && attribute.getNamespaceUri().equals(namespaceUri))
                return attribute;
        }
        return null;
    }

    public String getAttributeValue(String name) {
        return getAttributeValue("", name);
    }

    public String getAttributeValue(String namespaceUri, String name) {
        Attribute attribute = getAttribute(namespaceUri, name);
        if (attribute == null)
            return null;
        else
            return attribute.getValue();
    }

    public Map<String, String> getDeclaredNamespaces() {
        return namespaces;
    }

    public String getXMLLang() {
        return getAttributeValue(Namespaces.XML, "lang");
    }

    public List<XMLFragment> getInnerFragments() {
        return Collections.unmodifiableList(innerFragments);
    }

    public XMLElement getFirstInnerElement() {
        if (innerFragments == null || innerFragments.size() < 1)
            return null;
        for (XMLFragment xmlFragment : innerFragments) {
            if (xmlFragment instanceof XMLElement)
                return (XMLElement) xmlFragment;
        }
        return null;
    }

    public List<XMLElement> getInnerElements() {
        if (innerFragments == null || innerFragments.size() < 1)
            return Collections.emptyList();
        List<XMLElement> innerElements = new ArrayList<XMLElement>();
        for (XMLFragment xmlFragment : innerFragments) {
            if (xmlFragment instanceof XMLElement)
                innerElements.add((XMLElement) xmlFragment);
        }
        return innerElements;
    }

    public List<XMLText> getInnerTexts() {
        if (innerFragments == null || innerFragments.size() < 1)
            return Collections.emptyList();
        List<XMLText> innerTexts = new ArrayList<XMLText>();
        for (XMLFragment xmlFragment : innerFragments) {
            if (xmlFragment instanceof XMLText)
                innerTexts.add((XMLText) xmlFragment);
        }
        return innerTexts;
    }

    public XMLText getFirstInnerText() {
        if (innerFragments == null || innerFragments.size() < 1)
            return null;
        for (XMLFragment xmlFragment : innerFragments) {
            if (xmlFragment instanceof XMLText)
                return (XMLText) xmlFragment;
        }
        return null;
    }

    public XMLText getSingleInnerText() throws XMLSemanticError {
        List<XMLText> innerTexts = getInnerTexts();
        if (innerTexts == null || innerTexts.isEmpty())
            return null;
        if (innerTexts.size() > 1)
            throw new XMLSemanticError("element has more than one inner text fragment");
        return innerTexts.get(0);
    }

    /**
     * Get the complete inner text
     * @return The concatenated inner text or null if no text fragments exist
     */
    public XMLText getInnerText() {
        boolean hadText = false;
        StringBuffer sb = new StringBuffer();
        for (XMLText text : getInnerTexts()) {
            sb.append(text.getText());
            hadText = true;
        }
        if (hadText) {
            return new XMLText(sb.toString());
        } else {
            return null;
        }

    }

    /**
     * collects all inner elements named as given parameter. Namespace UIR is ignored.
     * @param name - must not be NULL
     */
    public List<XMLElement> getInnerElementsNamed(String name) {
        return getInnerElementsNamed(name, null);
    }

    /**
     * collects all inner elements named as given parameter
     * @param name - must not be NULL
     * @param namespaceUri The namespace URI used for matching. Null if namespace URIs should not be considered
     */
    public List<XMLElement> getInnerElementsNamed(String name, String namespaceUri) {
        if (name == null)
            return null;
        List<XMLElement> innerElements = getInnerElements();
        if (innerElements == null)
            return null;
        if (innerElements.size() == 0)
            return innerElements;
        Iterator<XMLElement> elementIterator = innerElements.iterator(); // this List will be modified now!
        while (elementIterator.hasNext()) {
            XMLElement xmlElement = elementIterator.next();
            if (!name.equals(xmlElement.getName())
                    || (namespaceUri != null && !namespaceUri.equals(xmlElement.getNamespaceURI()))) {
                elementIterator.remove();
            }
        }
        return innerElements;
    }

    public XMLElement getSingleInnerElementsNamed(String name) throws XMLSemanticError {
        return getSingleInnerElementsNamed(name, null);
    }

    public XMLElement getSingleInnerElementsNamed(String name, String namespaceUri) throws XMLSemanticError {
        List<XMLElement> innerElements = getInnerElementsNamed(name, namespaceUri);
        if (innerElements == null)
            return null;
        if (innerElements.isEmpty())
            return null;
        if (innerElements.size() > 1)
            throw new XMLSemanticError("element has more than one inner element named: " + name);
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
            XMLElement xmlElement = elementIterator.next();
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
        if (this == o)
            return true;
        if (o == null || !(o instanceof XMLElement))
            return false;

        final XMLElement that = (XMLElement) o;

        // attributes are allowed to be in any order
        if(attributes != null && that.attributes != null) {
            if(attributes.size() != that.attributes.size()) return false;
            for(Attribute attribute : attributes) {
                boolean found = false;
                for(Attribute thatAttribute : that.attributes) {
                    if(thatAttribute.equals(attribute)) {
                        found = true;
                        break;
                    }
                }
                if(!found) return false;
            }
            
        } else if(attributes == null && that.attributes == null) {
            // ok
        } else {
            return false;
        }
        
        if (innerFragments != null ? !innerFragments.equals(that.innerFragments) : that.innerFragments != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;

        // TODO the namespace prefix should not matter for equality, only the URI
        if (namespacePrefix != null ? !namespacePrefix.equals(that.namespacePrefix) : that.namespacePrefix != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (namespacePrefix != null ? namespacePrefix.hashCode() : 0);
        result = 29 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 29 * result + (innerFragments != null ? innerFragments.hashCode() : 0);
        return result;
    }

    public XMLElementVerifier getVerifier() {
        if (xmlElementVerifier == null)
            xmlElementVerifier = new XMLElementVerifier(this);
        return xmlElementVerifier;
    }
}
