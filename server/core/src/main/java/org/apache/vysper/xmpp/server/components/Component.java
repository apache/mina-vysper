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
package org.apache.vysper.xmpp.server.components;

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.StanzaHandler;

/**
 * a component is a server subsystem providing a dedicated extension. components
 * operate on their own subdomain, e.g. conference.vysper.org for MUC.
 * components have a dedicated context in which they receive stanzas
 */
public interface Component {

    /**
     * the subdomain this component should become available under. example:
     * subdomain = 'chat' + server domain = 'vysper.org' => 'chat.vysper.org'
     * 
     * @return
     */
    String getSubdomain();

    List<StanzaHandler> getComponentHandlers(Entity fullDomain);

    List<NamespaceHandlerDictionary> getComponentHandlerDictionnaries(Entity fullDomain);
}
