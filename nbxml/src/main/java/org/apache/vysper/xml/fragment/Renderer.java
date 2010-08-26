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

import java.util.Map;
import java.util.Map.Entry;

/**
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

        ResolverNamespaceResolver nsResolver = new ResolverNamespaceResolver();
        renderXMLElement(topElement, nsResolver, openElementBuffer, elementContentBuffer, closeElementBuffer);
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

    private void renderXMLElement(XMLElement element, ResolverNamespaceResolver nsResolver,
            StringBuilder openElementBuffer, StringBuilder elementContentBuffer, StringBuilder closeElementBuffer) {
        nsResolver.push(element);

        openElementBuffer.append("<");
        renderElementName(openElementBuffer, element, nsResolver);

        // render namespace declarations
        Map<String, String> nsAttrs = nsResolver.getNamespaceDeclarations();
        for (Entry<String, String> nsAttr : nsAttrs.entrySet()) {
            openElementBuffer.append(" ");
            String name;
            if (nsAttr.getKey().length() == 0) {
                name = "xmlns";
            } else {
                name = "xmlns:" + nsAttr.getKey();
            }
            renderAttribute(openElementBuffer, name, nsAttr.getValue());
        }

        for (Attribute attribute : element.getAttributes()) {
            // make sure we do not render namespace attributes,
            // nor normal attributes containing namespace declarations (probably due to
            // the parser not correctly creating namespace attributes for these which are then 
            // copied into for example error responses)

            if (!attribute.getName().startsWith("xmlns")) {
                openElementBuffer.append(" ");
                renderAttribute(openElementBuffer, attribute, nsResolver);
            }
        }
        openElementBuffer.append(">");
        for (XMLFragment xmlFragment : element.getInnerFragments()) {
            if (xmlFragment instanceof XMLElement) {
                renderXMLElement((XMLElement) xmlFragment, nsResolver, elementContentBuffer, elementContentBuffer,
                        elementContentBuffer);
            } else if (xmlFragment instanceof XMLText) {
                elementContentBuffer.append(escapeTextValue(((XMLText) xmlFragment).getText()));
            } else if(xmlFragment == null) {
                // ignore
            } else {
                throw new UnsupportedOperationException("cannot render XML fragment of type "
                        + xmlFragment.getClass().getName());
            }
        }

        closeElementBuffer.append("</");
        renderElementName(closeElementBuffer, element, nsResolver);
        closeElementBuffer.append(">");
        // remove this element from the NS resolver stack
        nsResolver.pop();
    }

    private boolean hasXmlnsReservedName(Attribute attribute) {
        String name = attribute.getName();
        return name.equals("xmlns") || name.startsWith("xmlns:");
    }

    private void renderElementName(StringBuilder buffer, XMLElement element, ResolverNamespaceResolver nsResolver) {
        // if the element has a namespace prefix, retrieves the prefix from the defining attribute
        if (element.getNamespacePrefix() != null && element.getNamespacePrefix().length() > 0) {
            buffer.append(element.getNamespacePrefix()).append(COLON);
        } else if (element.getNamespaceURI().length() > 0) {
            // element is in a namespace, but without a declared prefix, we need to resolve the prefix
            String prefix = nsResolver.resolvePrefix(element.getNamespaceURI());
            if (prefix != null && prefix.length() > 0) {
                buffer.append(prefix).append(COLON);
            }
        }

        buffer.append(element.getName());
    }

    private void renderAttribute(StringBuilder buffer, Attribute attribute, ResolverNamespaceResolver nsResolver) {
        String qname;
        if (!attribute.getNamespaceUri().equals("")) {
            // attribute is in a namespace, resolve prefix
            qname = nsResolver.resolvePrefix(attribute.getNamespaceUri()) + ":" + attribute.getName();
        } else {
            qname = attribute.getName();
        }

        renderAttribute(buffer, qname, attribute.getValue());
    }

    private void renderAttribute(StringBuilder buffer, String qname, String value) {
        buffer.append(qname).append("=\"").append(escapeAttributeValue(value)).append("\"");
    }

    private String escapeAttributeValue(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String escapeTextValue(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

}
