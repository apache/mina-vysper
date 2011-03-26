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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.vysper.xml.fragment.Namespaces;

/**
 * Naive implementation, will be replaced in later stages of this change
 */
public class ParserNamespaceResolver {

    private Stack<Map<String, String>> elements = new Stack<Map<String, String>>();

    public ParserNamespaceResolver() {
    }

    public void push(Map<String, String> elmXmlns) {
        elements.push(elmXmlns);
    }

    public void pop() {
        elements.pop();
    }

    public String resolveUri(String prefix) {
        // check for the reserved xml namespace
        if (prefix.equals("xml")) {
            return Namespaces.XML;
        } else {
            // walk over the stack backwards
            for (int i = elements.size() - 1; i >= 0; i--) {
                Map<String, String> ns = elements.get(i);
                if (ns.containsKey(prefix)) {
                    return ns.get(prefix);
                }
            }
        }

        // could not resolve URI
        return null;
    }

    public String resolvePrefix(String uri) {
        if (uri.equals(Namespaces.XML)) {
            return "xml";
        } else {
            // walk over the stack backwards
            for (int i = elements.size() - 1; i >= 0; i--) {
                Map<String, String> ns = elements.get(i);
                for (Entry<String, String> entry : ns.entrySet()) {
                    if (entry.getValue().equals(uri)) {
                        return entry.getKey();
                    }
                }
            }
        }

        return null;
    }
}
