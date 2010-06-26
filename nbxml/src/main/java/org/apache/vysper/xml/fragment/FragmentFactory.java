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
import java.util.Arrays;
import java.util.List;

/**
 * convenience methods for creating XML fragments
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class FragmentFactory {
    public static XMLElement createElementWithInnerText(String name, String text) {
        return createElementWithInnerText(null, name, text);
    }

    public static XMLElement createElementWithInnerText(String namespaceURI, String name, String text) {
        return new XMLElement(namespaceURI, name, null, null, asList(new XMLText(text)), null);
    }

    public static List<XMLFragment> asList(XMLFragment xmlFragment) {
        List<XMLFragment> xmlFragments = new ArrayList<XMLFragment>();
        if (xmlFragment != null)
            xmlFragments.add(xmlFragment);
        return xmlFragments;
    }

    public static List<Attribute> asList(Attribute attribute) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        if (attribute != null)
            attributes.add(attribute);
        return attributes;
    }

    public static List<XMLFragment> asList(XMLFragment[] xmlFragmentArray) {
        if (xmlFragmentArray == null)
            return new ArrayList<XMLFragment>();
        return Arrays.asList(xmlFragmentArray);
    }

    public static List<Attribute> asList(Attribute[] attributeArray) {
        if (attributeArray == null)
            return new ArrayList<Attribute>();
        return Arrays.asList(attributeArray);
    }
}
