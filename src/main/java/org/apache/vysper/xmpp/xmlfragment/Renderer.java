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

/**
 * TODO support namespaces (inherited from outer/inheriting for inner elements)
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Renderer {

    private XMLElement topElement;
    private StringBuilder openElementBuffer = new StringBuilder();
    private StringBuilder elementContentBuffer = new StringBuilder();
    private StringBuilder closeElementBuffer = new StringBuilder();
    private static final String COLON = ":";

    public Renderer(XMLElement element) {
        this.topElement = element;
        renderXMLElement(topElement, openElementBuffer, elementContentBuffer, closeElementBuffer);

    }

    public String getOpeningElement() {
        return openElementBuffer.toString();
    }

    public String getElementContent() {
        return elementContentBuffer.toString();
    }

    public String getClosingElement() {
        return closeElementBuffer.toString();
    }

    public String getComplete() {
        return openElementBuffer.toString() + elementContentBuffer.toString() + closeElementBuffer.toString();
    }

    private void renderXMLElement(XMLElement element, StringBuilder openElementBuffer, StringBuilder elementContentBuffer, StringBuilder closeElementBuffer) {
        String name = element.getName();
        String namespacePrefix = element.getNamespacePrefix();

        openElementBuffer.append("<");
        renderElementName(openElementBuffer, element, namespacePrefix, name);
        for (Attribute attribute : element.getAttributes()) {
            openElementBuffer.append(" ");
            renderAttribute(openElementBuffer, attribute);
        }
        openElementBuffer.append(">");

        for (XMLFragment xmlFragment : element.getInnerFragments()) {
            if (xmlFragment instanceof XMLElement) renderXMLElement((XMLElement) xmlFragment, elementContentBuffer, elementContentBuffer, elementContentBuffer);
            else if (xmlFragment instanceof XMLText) {
                elementContentBuffer.append(((XMLText) xmlFragment).getText());
            } else {
                throw new UnsupportedOperationException("cannot render XML fragment of type " + xmlFragment.getClass().getName());
            }
        }

        closeElementBuffer.append("</");
        renderElementName(closeElementBuffer, element, namespacePrefix, name);
        closeElementBuffer.append(">");

    }

    private void renderElementName(StringBuilder buffer, XMLElement element, String namespacePrefix, String name) {
        // if the element has a namespace prefix, retrieves the prefix from the defining attribute
        if (namespacePrefix != null) {
            String namespaceIdentifier = element.getVerifier().getNamespaceIdentifier(namespacePrefix);
            if (namespaceIdentifier != null) {
                buffer.append(namespaceIdentifier).append(COLON);
            }
        }

        buffer.append(name);
    }

    private void renderAttribute(StringBuilder buffer, Attribute attribute) {
        buffer.append(attribute.getName()).append("=\"").append(attribute.getValue()).append("\"");
    }
}
