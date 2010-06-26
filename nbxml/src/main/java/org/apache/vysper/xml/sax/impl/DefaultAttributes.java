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
package org.apache.vysper.xml.sax.impl;

import java.util.Collections;
import java.util.List;

import org.xml.sax.Attributes;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultAttributes implements Attributes {

    private List<Attribute> attributes;

    public DefaultAttributes() {
        this.attributes = Collections.emptyList();
    }

    public DefaultAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public int getIndex(String qName) {
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.get(i);
            if (qName.equals(attribute.getQname()))
                return i;
        }

        return -1;
    }

    public int getIndex(String uri, String localName) {
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.get(i);
            if (uri.equals(attribute.getURI()) && localName.equals(attribute.getLocalName()))
                return i;
        }

        return -1;
    }

    public int getLength() {
        return attributes.size();
    }

    public String getLocalName(int index) {
        if (index < 0 || index >= attributes.size())
            return null;

        return attributes.get(index).getLocalName();
    }

    public String getQName(int index) {
        if (index < 0 || index >= attributes.size())
            return null;

        return attributes.get(index).getQname();
    }

    public String getType(int index) {
        if (index < 0 || index >= attributes.size())
            return null;

        return "CDATA";
    }

    public String getType(String qName) {
        return getType(getIndex(qName));
    }

    public String getType(String uri, String localName) {
        return getType(getIndex(uri, localName));
    }

    public String getURI(int index) {
        if (index < 0 || index >= attributes.size())
            return null;

        return attributes.get(index).getURI();
    }

    public String getValue(int index) {
        if (index < 0 || index >= attributes.size())
            return null;
        return attributes.get(index).getValue();
    }

    public String getValue(String qName) {
        return getValue(getIndex(qName));
    }

    public String getValue(String uri, String localName) {
        return getValue(getIndex(uri, localName));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        DefaultAttributes other = (DefaultAttributes) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;

        return true;
    }

}