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

package org.apache.vysper.mina.codec;

import java.util.List;

import org.apache.vysper.xml.decoder.XMLElementBuilderFactory;
import org.apache.vysper.xml.fragment.AbstractXMLElementBuilder;
import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaBuilderFactory extends XMLElementBuilderFactory {

    public AbstractXMLElementBuilder<StanzaBuilder, Stanza> createBuilder(String elementName, String namespaceURI,
            String namespacePrefix, List<Attribute> attributes, List<XMLFragment> innerFragments) {
        return new StanzaBuilder(elementName, namespaceURI, namespacePrefix, attributes, innerFragments);
    }
}
