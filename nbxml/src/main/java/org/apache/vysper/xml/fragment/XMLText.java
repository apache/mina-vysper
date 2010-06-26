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
 * immutable text section (as it can appear between two xml elements
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLText implements XMLFragment {
    private String text;

    public XMLText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final XMLText xmlText = (XMLText) o;

        if (text != null ? !text.equals(xmlText.text) : xmlText.text != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (text != null ? text.hashCode() : 0);
    }

    @Override
    public String toString() {
        return getText();
    }
}
