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

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * a collection of stanza handlers belonging together in a semantical way for example because they all
 * handle the same namespace.
 * @see org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface HandlerDictionary {

    /**
     * adds a new handler to this dictionary
     * @param stanzaHandler
     */
    void register(StanzaHandler stanzaHandler);

    /**
     * no additional handler can be added after invoking this method
     */
    void seal();

    /**
     * retrieves the handler for a stanza by inspecting the stanza.
     * if the stanza cannot be handled, returns null.
     * @param stanza
     * @return NULL, if dictionary contains no handler for this stanza, or the handler otherwise
     */
    StanzaHandler get(Stanza stanza);
}
