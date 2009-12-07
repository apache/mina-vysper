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
package org.apache.vysper.xmpp.writer;

import org.apache.vysper.xml.fragment.*;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Iterator;

/**
 * renders only reduced digest stanza information for logging output
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DenseStanzaLogRenderer {

    final static Logger logger = LoggerFactory.getLogger(DenseStanzaLogRenderer.class);

    private static final String ELEMENT_SEPARATOR = ".";
    private static final String ATTR_QUOTE = "'";
    private static final String EQUALS = "=";

    public static String render(XMLElement stanza) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            if (stanza == null) return stringBuilder.append("NULL_STANZA").toString();
            String outerName = stanza.getName();
            stringBuilder.append(outerName);
            XMLElement firstInnerElement = stanza.getFirstInnerElement();

            if ("stream".equals(outerName)) {
                renderStreamStart(stringBuilder, stanza, firstInnerElement);
            } else if ("message".equals(outerName)) {
                renderMessage(stringBuilder, stanza, firstInnerElement);
            } else if ("error".equals(outerName)) {
                renderError(stringBuilder, stanza, firstInnerElement);
            } else if ("presence".equals(outerName)) {
                renderPresence(stringBuilder, stanza, firstInnerElement);
            } else if ("auth".equals(outerName)) {
                renderAuth(stringBuilder, stanza, firstInnerElement);
            } else if ("iq".equals(outerName)) {
                renderIQ(stringBuilder, stanza, firstInnerElement);
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            logger.warn("error when rendering stanza {}: {}", stanza.toString(), e);
            return "*render-exception*";
        }
    }

    private static void renderIQ(StringBuilder stringBuilder, XMLElement stanza, XMLElement firstInnerElement) {
        IQStanza iq = (IQStanza) XMPPCoreStanza.getWrapper((Stanza) stanza);
        renderAttribute(stringBuilder, iq, "id");
        renderAttribute(stringBuilder, iq, "to");
        renderAttribute(stringBuilder, iq, "from");
        renderAttribute(stringBuilder, iq, "type");
        if (firstInnerElement != null) {
            stringBuilder.append(ELEMENT_SEPARATOR).append(firstInnerElement.getName());
            if ("query".equals(firstInnerElement.getName())) {
                renderIQQuery(stringBuilder, firstInnerElement);
            } else {
                renderAttribute(stringBuilder, firstInnerElement, NamespaceAttribute.XMLNS);
            }
        }
    }

    private static void renderIQQuery(StringBuilder stringBuilder, XMLElement queryElement) {
        Attribute attribute = queryElement.getAttribute(NamespaceAttribute.XMLNS);
        if (attribute == null) {
            stringBuilder.append(ELEMENT_SEPARATOR).append("?").append(NamespaceAttribute.XMLNS).append("?");
            return;
        }
        String xmlns = attribute.getValue();
        if (!xmlns.startsWith("http://jabber.org/protocol/disco")) {
            renderAttribute(stringBuilder, queryElement, NamespaceAttribute.XMLNS);
            return;
        }
        stringBuilder.append(ELEMENT_SEPARATOR).append("disco");
        if (xmlns.equals("http://jabber.org/protocol/disco#items")) {
            stringBuilder.append(ELEMENT_SEPARATOR).append("items");
        } else if (xmlns.equals("http://jabber.org/protocol/disco#info")) {
            stringBuilder.append(ELEMENT_SEPARATOR).append("info");
            List<XMLElement> features = queryElement.getInnerElementsNamed("feature");
            if (features != null) for (XMLElement feature : features) {
                String varAttrValue = feature.getAttributeValue("var");
                if (varAttrValue == null) varAttrValue = "NOT_GIVEN";
                renderAttributeForm(stringBuilder, "feature", varAttrValue);
            }
        } else {
            stringBuilder.append(ELEMENT_SEPARATOR).append("????");
        }
        // TODO render inner elements in case of result etc.
    }

    private static void renderAuth(StringBuilder stringBuilder, XMLElement stanza, XMLElement firstInnerElement) {
        renderAttribute(stringBuilder, stanza, "mechanism");
    }

    private static void renderStreamStart(StringBuilder stringBuilder, XMLElement stanza, XMLElement firstInnerElement) {
        try {
            XMLElement features = stanza.getSingleInnerElementsNamed("features");
            if (features != null) {
                XMLElement starttls = features.getSingleInnerElementsNamed("starttls");
                if (starttls != null) {
                    stringBuilder.append(ELEMENT_SEPARATOR);
                    stringBuilder.append("starttls");
                    XMLElement required = starttls.getSingleInnerElementsNamed("required");
                    if (required != null) stringBuilder.append("[required]");
                }
                XMLElement mechanisms = features.getSingleInnerElementsNamed("mechanisms");
                if (mechanisms != null) {
                    stringBuilder.append(ELEMENT_SEPARATOR);
                    stringBuilder.append("features.mechanisms[");
                    List<XMLElement> list = mechanisms.getInnerElementsNamed("mechanism");
                    if (list != null) {
                        for (Iterator<XMLElement> it = list.iterator(); it.hasNext();) {
                            XMLElement element = it.next();
                            stringBuilder.append(element.getSingleInnerText().getText());
                            if (it.hasNext()) stringBuilder.append(",");
                        }
                    }
                    stringBuilder.append("]");
                }
            }
        } catch (XMLSemanticError xmlSemanticError) {
            stringBuilder.append("*error*");
        }
    }

    private static void renderError(StringBuilder stringBuilder, XMLElement stanza, XMLElement firstInnerElement) {
        if (firstInnerElement == null) {
            stringBuilder.append(ELEMENT_SEPARATOR);
            stringBuilder.append("???");
            return;
        }

        stringBuilder.append(ELEMENT_SEPARATOR);
        stringBuilder.append(firstInnerElement.getName());
    }

    private static void renderPresence(StringBuilder stringBuilder, XMLElement stanza, XMLElement firstInnerElement) {
        renderAttribute(stringBuilder, stanza, "type");
        renderAttribute(stringBuilder, stanza, "from");
        renderAttribute(stringBuilder, stanza, "to");

        try {
            XMLElement show = stanza.getSingleInnerElementsNamed("show");
            if (show != null) {
                XMLText showText = show.getSingleInnerText();
                if (showText != null) {
                    stringBuilder.append(ELEMENT_SEPARATOR);
                    stringBuilder.append("show").append(EQUALS);
                    stringBuilder.append(ATTR_QUOTE).append(showText).append(ATTR_QUOTE);
                }
            }
            XMLElement status = stanza.getSingleInnerElementsNamed("status");
            if (status != null) {
                XMLText statusText = status.getSingleInnerText();
                if (statusText != null) {
                    stringBuilder.append(ELEMENT_SEPARATOR);
                    stringBuilder.append("status").append(EQUALS);
                    stringBuilder.append(ATTR_QUOTE).append(statusText).append(ATTR_QUOTE);
                }
            }
            XMLElement caps = stanza.getSingleInnerElementsNamed("c");
            if (caps != null) {
                renderAttribute(stringBuilder, caps, "node");
                renderAttribute(stringBuilder, caps, "ver");
                renderAttribute(stringBuilder, caps, "ext");
            }

        } catch (XMLSemanticError xmlSemanticError) {
            stringBuilder.append("*error*");
        }

    }

    private static void renderAttribute(StringBuilder stringBuilder, XMLElement element, String attributeName) {
        String attributeValue = element.getAttributeValue(attributeName);
        if (attributeValue != null) {
            renderAttributeForm(stringBuilder, attributeName, attributeValue);
        }
    }

    private static void renderAttributeForm(StringBuilder stringBuilder, String attributeName, String value) {
        stringBuilder.append(ELEMENT_SEPARATOR);
        stringBuilder.append(attributeName).append(EQUALS);
        stringBuilder.append(ATTR_QUOTE).append(value).append(ATTR_QUOTE);
    }

    private static void renderMessage(StringBuilder stringBuilder, XMLElement stanza, XMLElement firstInnerElement) {
        if (firstInnerElement != null) {
            stringBuilder.append(ELEMENT_SEPARATOR);
            stringBuilder.append(firstInnerElement.getName());

            String firstInnerName = firstInnerElement.getName();
            if ("body".equals(firstInnerName)) {
                stringBuilder.append(ELEMENT_SEPARATOR);
                XMLText xmlText = null;
                try {
                    xmlText = firstInnerElement.getSingleInnerText();
                    if (xmlText != null) stringBuilder.append(xmlText.getText());
                } catch (XMLSemanticError xmlSemanticError) {
                    stringBuilder.append("???");
                }
            }
        }
    }
}
