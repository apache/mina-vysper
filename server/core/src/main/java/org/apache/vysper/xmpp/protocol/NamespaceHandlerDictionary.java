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
package org.apache.vysper.xmpp.protocol;

import java.util.List;

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * holds all stanza handlers for a distinct namespace
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class NamespaceHandlerDictionary extends DefaultHandlerDictionary {

    private String namespaceURI;

    public NamespaceHandlerDictionary(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    public NamespaceHandlerDictionary(String namespaceURI, List<StanzaHandler> handlerList) {
        super(handlerList);
        this.namespaceURI = namespaceURI;
    }

    public NamespaceHandlerDictionary(String namespaceURI, StanzaHandler stanzaHandler) {
        super(stanzaHandler);
        this.namespaceURI = namespaceURI;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    @Override
    public StanzaHandler get(Stanza stanza) {
        String namespace;
        if(stanza.getVerifier().subElementsPresentExact(1)) {
            namespace = stanza.getFirstInnerElement().getNamespaceURI();
        } else {
            namespace = stanza.getNamespaceURI();
        }
        
        if(namespace != null && namespace.equals(namespaceURI)) {
            return super.get(stanza);
        } else {
            return null;
        }
    }
}
