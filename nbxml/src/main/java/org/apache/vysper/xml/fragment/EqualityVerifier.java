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

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class EqualityVerifier {

    private static AttributeComparator ATTRIBUTE_COMPARATOR = new AttributeComparator();

    /**
     * checks only name, namespace and attributes of both XMLElements for equality,
     * but not inner elements
     */
    public boolean equalsElements_Shallow(XMLElement first, XMLElement second) {
        if (first == null)
            return second == null;
        if (second == null)
            return false;

        if (!first.getName().equals(second.getName()))
            return false;
        if (!first.getNamespacePrefix().equals(second.getNamespacePrefix()))
            return false;

        return equalsAttributes(first.getAttributes(), second.getAttributes());
    }

    /**
     * additional to the shallow check, does check the inner elements. more precisely, it checks whether
     * all inner elements at corresponding positions are equal. it does _not_ check whether inner elements
     * are the same for some proper mutation.
     */
    public boolean equalsElements_DeepSameOrder(XMLElement first, XMLElement second) {
        if (first == null)
            return second == null;
        if (second == null)
            return false;

        if (!first.getName().equals(second.getName()))
            return false;
        if (!first.getNamespacePrefix().equals(second.getNamespacePrefix()))
            return false;

        boolean attrEquals = equalsAttributes(first.getAttributes(), second.getAttributes());
        if (!attrEquals)
            return false;

        List<XMLElement> firstInnerElements = first.getInnerElements();
        List<XMLElement> secondInnerElements = second.getInnerElements();

        if (firstInnerElements.size() != secondInnerElements.size())
            return false;
        for (int i = 0; i < firstInnerElements.size(); i++) {
            if (!equalsElements_DeepSameOrder(firstInnerElements.get(0), secondInnerElements.get(0)))
                return false;
        }
        return true;
    }

    private boolean equalsAttributes(List<Attribute> firstAttrs, List<Attribute> secondAttrs) {
        TreeSet<Attribute> attrSorted1 = new TreeSet<Attribute>(ATTRIBUTE_COMPARATOR);
        attrSorted1.addAll(firstAttrs);

        TreeSet<Attribute> attrSorted2 = new TreeSet<Attribute>(ATTRIBUTE_COMPARATOR);
        attrSorted2.addAll(secondAttrs);

        return attrSorted1.equals(attrSorted2);
    }

    static class AttributeComparator implements Comparator<Attribute> {

        public int compare(Attribute attribute1, Attribute attribute2) {
            if (attribute1.getName().equals(attribute2.getName())) {
                return attribute1.getValue().compareTo(attribute2.getValue());
            }
            return attribute1.getName().compareTo(attribute2.getName());
        }
    }
}
