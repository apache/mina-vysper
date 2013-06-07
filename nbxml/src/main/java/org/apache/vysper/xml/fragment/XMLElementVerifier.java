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

import java.util.Collection;
import java.util.List;

import static org.apache.vysper.xml.fragment.Namespaces.XMLNS;
import static org.apache.vysper.xml.fragment.Namespaces.XMLNS_AND_COLON;

/**
 * provides common tools to check a element against its specification or
 * semantical context this classes instances are immutual.
 *
 * TODO add unit test
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLElementVerifier {
    protected XMLElement element;

    protected XMLElementVerifier(XMLElement element) {
        if (element == null)
            throw new IllegalArgumentException("null not allowed for element");
        this.element = element;
    }

    public boolean nameEquals(String name) {
        return element.getName().equals(name);
    }

    public boolean attributePresent(String name) {
        return attributePresent("", name);
    }

    public boolean attributePresent(String namespaceUri, String name) {
        return null != element.getAttribute(namespaceUri, name);
    }

    /**
     * Checks whether all given attributes are present on the element.
     *
     * @param names
     *            the attributes to check
     * @return true iff all attributes are present, false otherwise
     */
    public boolean allAttributesPresent(String... names) {
        if (names == null) {
            return false;
        }
        for (String name : names) {
            if (!attributePresent(name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether only the given attributes are present on the element.
     *
     * @param names
     *            the attributes to check
     * @return true iff only the given attributes are present, false otherwise
     */
    public boolean onlyAttributesPresent(String... names) {
        if (names == null) {
            return false;
        }
        return element.getAttributes().size() == names.length && allAttributesPresent(names);
    }

    /**
     * Checks whether any of the given attributes are present on the element.
     *
     * @param names
     *            the attributes to check
     * @return true iff at least one of the given attributes is present, false
     *         otherwise
     */
    public boolean anyAttributePresent(String... names) {
        if (names == null) {
            return false;
        }
        for (String name : names) {
            if (attributePresent(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean attributeEquals(String name, String value) {
        return attributeEquals("", name, value);
    }

    public boolean attributeEquals(String namespaceUri, String name, String value) {
        return attributePresent(namespaceUri, name) && element.getAttributeValue(namespaceUri, name).equals(value);
    }

    public boolean subElementPresent(String name) {
        for (XMLFragment xmlFragment : element.getInnerFragments()) {
            if (xmlFragment instanceof XMLElement) {
                XMLElement xmlElement = (XMLElement) xmlFragment;
                if (xmlElement.getName().equals(name))
                    return true;
            }
        }
        return false;
    }

    public boolean subElementsPresentExact(int numberOfSubelements) {
        return element.getInnerElements().size() == numberOfSubelements;
    }

    public boolean subElementsPresentAtLeast(int numberOfSubelements) {
        return element.getInnerElements().size() >= numberOfSubelements;
    }

    public boolean subElementsPresentAtMost(int numberOfSubelements) {
        return element.getInnerElements().size() <= numberOfSubelements;
    }

    public boolean namespacePresent(String namespaceURI) {
        Collection<String> nsUris = element.getDeclaredNamespaces().values();
        if (nsUris.contains(namespaceURI)) {
            return true;
        }

        for (Attribute attribute : element.getAttributes()) {
            if (attribute.getName().startsWith(XMLNS) && attribute.getValue().equals(namespaceURI)) {
                return true;
            }
        }

        if (namespaceURI.equals(element.getNamespaceURI()))
            return true;

        return false; // not present
    }

    /**
     * example for "http://myNS.org/anything", this method returns "myNS" for
     * element <test xmlns:myNS="http://myNS.org/anything" />
     *
     * @return the identifier for the given namespace definition
     */
    public String getNamespaceIdentifier(String namespace) {
        for (Attribute attribute : element.getAttributes()) {
            if (attribute.getValue().equals(namespace) && attribute.getName().startsWith(XMLNS_AND_COLON)) {
                return attribute.getName().substring(XMLNS_AND_COLON.length());
            }
        }
        return null;
    }

    private boolean isNamespaceAttribute(Attribute attribute) {
        return (attribute.getName().equalsIgnoreCase(XMLNS) || attribute.getName().startsWith(XMLNS_AND_COLON));
    }

    public String getUniqueXMLNSValue() {
        Attribute found = null;
        for (Attribute attribute : element.getAttributes()) {
            if (isNamespaceAttribute(attribute)) {
                if (found != null)
                    return null; // not unique
                else {
                    found = attribute;
                }
            }
        }
        if (found == null)
            return null;
        return found.getValue();
    }

    public boolean toAttributeEquals(String toValue) {
        return attributeEquals("to", toValue);
    }

    public boolean fromAttributeEquals(String fromValue) {
        return attributeEquals("from", fromValue);
    }

    public boolean onlySubelementEquals(String name, String namespaceURI) {
        List<XMLFragment> innerFragments = element.getInnerFragments();

        // really is only subelement
        if (innerFragments == null || innerFragments.size() != 1)
            return false;
        XMLFragment onlySubelement = innerFragments.get(0);
        if (!(onlySubelement instanceof XMLElement))
            return false;

        XMLElement xmlElement = ((XMLElement) onlySubelement);
        boolean nameEquals = name == null ? xmlElement.getName() == null : name.equals(xmlElement.getName());
        if (namespaceURI == null)
            namespaceURI = Namespaces.DEFAULT_NAMESPACE_URI;
        return nameEquals && namespaceURI.equals(xmlElement.getNamespaceURI());
    }
}
