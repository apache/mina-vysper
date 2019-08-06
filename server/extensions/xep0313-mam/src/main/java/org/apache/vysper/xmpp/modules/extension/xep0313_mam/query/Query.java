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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.query;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Optional;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.modules.extension.xep0059_result_set_management.Set;
import org.apache.vysper.xmpp.stanza.IQStanza;

/**
 * @author Réda Housni Alaoui
 */
public class Query {

    public static final String ELEMENT_NAME = "query";

    private final String namespace;

    private final IQStanza iqStanza;

    private final XMLElement element;

    public Query(String namespace, IQStanza iqStanza) throws XMLSemanticError {
        this.namespace = requireNonNull(namespace);
        this.iqStanza = iqStanza;
        this.element = iqStanza.getSingleInnerElementsNamed(ELEMENT_NAME);
        if (!ELEMENT_NAME.equals(element.getName())) {
            throw new IllegalArgumentException(
                    "Query element must be named '" + ELEMENT_NAME + "' instead of '" + element.getName() + "'");
        }
        if (!namespace.equals(element.getNamespaceURI())) {
            throw new IllegalArgumentException("Query element must be bound to namespace uri '" + namespace
                    + "' instead of '" + element.getNamespaceURI() + "'");
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public Optional<String> getQueryId() {
        return ofNullable(element.getAttributeValue("queryid"));
    }

    public Optional<String> getNode() {
        return ofNullable(element.getAttributeValue("node"));
    }

    public X getX() throws XMLSemanticError {
        return ofNullable(element.getSingleInnerElementsNamed(X.ELEMENT_NAME))
                .map(element1 -> new X(namespace, element1)).orElseGet(() -> X.empty(namespace));
    }

    public QuerySet getSet() throws XMLSemanticError {
        XMLElement setElement = element.getSingleInnerElementsNamed(Set.ELEMENT_NAME);
        if (setElement == null) {
            return QuerySet.empty();
        }
        return new QuerySet(new Set(setElement));
    }

    public IQStanza iqStanza() {
        return iqStanza;
    }

}
