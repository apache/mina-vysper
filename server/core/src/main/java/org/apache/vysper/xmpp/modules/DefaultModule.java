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
package org.apache.vysper.xmpp.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.vysper.event.EventListenerDictionary;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */

public abstract class DefaultModule implements Module {

    public abstract String getName();

    public abstract String getVersion();

    public List<HandlerDictionary> getHandlerDictionaries() {
        List<HandlerDictionary> dictionary = new ArrayList<HandlerDictionary>();
        addHandlerDictionaries(dictionary);
        return dictionary;
    }

    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        // empty default implementation
    }

    @Override
    public List<StanzaHandlerInterceptor> getStanzaHandlerInterceptors() {
        return Collections.emptyList();
    }

    @Override
    public Optional<EventListenerDictionary> getEventListenerDictionary() {
        return Optional.empty();
    }

    public List<ServerRuntimeContextService> getServerServices() {
        List<ServerRuntimeContextService> serviceList = new ArrayList<ServerRuntimeContextService>();
        addServerServices(serviceList);
        return serviceList;
    }

    protected void addServerServices(List<ServerRuntimeContextService> serviceList) {
        // empty default implementation
    }

    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        // empty default implementation
    }

    public void close() {
        // empty default implementation
    }
}
