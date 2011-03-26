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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

/**
 * Naive implementation, will be replaced in later stages of this change
 */
public class ResolverNamespaceResolver {

    private Stack<XMLElement> elements = new Stack<XMLElement>();

    public ResolverNamespaceResolver() {
    }

    public void push(XMLElement elm) {
        elements.push(elm);
    }

    public void pop() {
        elements.pop();
    }

    public Map<String, String> getNamespaceDeclarations() {
        Map<String, String> ns = new LinkedHashMap<String, String>();
        if (!elements.isEmpty()) {
            XMLElement topElm = elements.peek();
            for (Entry<String, String> entry : topElm.getDeclaredNamespaces().entrySet()) {
                ns.put(entry.getKey(), entry.getValue());
            }

            // look for any generated prefixes for attributes
            for (Attribute attribute : topElm.getAttributes()) {
                String attrNs = attribute.getNamespaceUri();
                if (attrNs.length() > 0 && resolvePrefix(attrNs, false) == null) {
                    // attribute is in a namespace, and that namespace is not declared
                    ns.put(resolvePrefix(attrNs), attrNs);
                }
            }

            // add the element declaration as well, if not already defined
            if (topElm.getNamespaceURI().length() > 0) {
                // declared in a namespace
                if (!ns.containsValue(topElm.getNamespaceURI())) {
                    // the namespace is not declared as an attribute on topElm
                    // is it declared on a parent?
                    // pop the element to test for declaration on the parent
                    pop();
                    if (resolvePrefix(topElm.getNamespaceURI(), false) == null) {
                        ns.put(topElm.getNamespacePrefix(), topElm.getNamespaceURI());
                    }
                    // restore stack
                    push(topElm);
                }
            } else {
                // is there a parent in a namespace? if so, we need to reset the default namespace
                if (elements.size() > 1 && elements.get(elements.size() - 2).getNamespaceURI().length() > 0
                        && !ns.containsKey("")) {
                    ns.put("", "");
                }
            }
        }

        return ns;
    }

    public String resolveUri(String prefix) {
        // check for the reserved xml namespace
        if (prefix.equals("xml")) {
            return Namespaces.XML;
        } else {
            for (int i = elements.size() - 1; i >= 0; i--) {
                String uri = resolveUri(elements.get(i), prefix);
                if (uri != null) {
                    return uri;
                }
            }
        }

        // is the current element declared with this prefix, 
        // if so, return the declared namespace
        if (!elements.isEmpty() && prefix.equals(elements.peek().getNamespacePrefix())) {
            // the namespace URI must not be defined for another prefix
            String uri = elements.peek().getNamespaceURI();
            String resolvedPrefix = resolvePrefix(uri, false);
            if (resolvedPrefix == null || resolvedPrefix.equals(prefix)) {
                return uri;
            }
        }

        // could not resolve URI
        return null;
    }

    private String resolveUri(XMLElement elm, String prefix) {
        return elm.getDeclaredNamespaces().get(prefix);
    }

    public String resolvePrefix(String uri) {
        return resolvePrefix(uri, true);
    }

    private String resolvePrefix(String uri, boolean generatePrefix) {
        // check for the reserved xml namespace
        if (uri.equals(Namespaces.XML)) {
            return "xml";
        } else {
            for (int i = elements.size() - 1; i >= 0; i--) {
                String prefix = resolvePrefix(elements.get(i), uri);
                if (prefix != null) {
                    return prefix;
                }
            }
        }

        // is the current element in this namespace, if so, return the default prefix
        if (!elements.isEmpty() && uri.equals(elements.peek().getNamespaceURI())) {
            return elements.peek().getNamespacePrefix();
        }

        if (generatePrefix) {
            // TODO replace with prefix generation
            return "ns1";
        } else {
            return null;
        }

    }

    private String resolvePrefix(XMLElement elm, String uri) {
        for (Entry<String, String> entry : elm.getDeclaredNamespaces().entrySet()) {
            if (entry.getValue().equals(uri)) {
                return entry.getKey();
            }
        }

        return null;
    }
}
