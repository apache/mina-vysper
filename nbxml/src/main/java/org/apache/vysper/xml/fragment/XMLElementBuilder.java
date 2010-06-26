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

import java.util.List;
import java.util.Map;

/**
 * TODO For now, this is mostly a copy of StanzaBuilder. Both classes needs to be refactored.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLElementBuilder extends AbstractXMLElementBuilder<XMLElementBuilder, XMLElement> {

    public XMLElementBuilder(String elementName) {
        this(elementName, null);
    }

    public XMLElementBuilder(String elementName, String namespaceURI) {
        this(elementName, namespaceURI, null);
    }

    public XMLElementBuilder(String elementName, String namespaceURI, String namespacePrefix) {
        super(elementName, namespaceURI, namespacePrefix);
    }

    public XMLElementBuilder(String elementName, String namespaceURI, String namespacePrefix,
            List<Attribute> attributes, List<XMLFragment> innerFragments) {
        super(elementName, namespaceURI, namespacePrefix, attributes, null, innerFragments);
    }

    public XMLElementBuilder(String elementName, String namespaceURI, String namespacePrefix,
            List<Attribute> attributes, Map<String, String> namespaces, List<XMLFragment> innerFragments) {
        super(elementName, namespaceURI, namespacePrefix, attributes, namespaces, innerFragments);
    }
}
