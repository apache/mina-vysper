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

/**
 * represents an XML element's attribute
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Attribute {
    private String namespaceUri;

    private String name;

    private String value;

    public Attribute(String name, String value) {
        this(null, name, value);
    }

    public Attribute(String namespaceUri, String name, String value) {
        if (name == null)
            throw new IllegalArgumentException("name must not be null");
        if (value == null)
            throw new IllegalArgumentException("value must not be null");
        this.namespaceUri = namespaceUri == null ? Namespaces.DEFAULT_NAMESPACE_URI : namespaceUri;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof Attribute))
            return false;

        final Attribute attribute = (Attribute) o;

        if (namespaceUri != null ? !namespaceUri.equals(attribute.namespaceUri) : attribute.namespaceUri != null)
            return false;
        if (name != null ? !name.equals(attribute.name) : attribute.name != null)
            return false;
        if (value != null ? !value.equals(attribute.value) : attribute.value != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (namespaceUri != null ? namespaceUri.hashCode() : 0);
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
