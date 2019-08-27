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

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ComponentStanzaProcessorFactory;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleComponentRegistry implements AlterableComponentRegistry {

    private final Entity serverEntity;

    /**
     * map of all registered components, index by the subdomain they are registered
     * for
     */
    private final Map<String, StanzaProcessor> processorBySubdomain = new HashMap<>();

    public SimpleComponentRegistry(Entity serverEntity) {
        this.serverEntity = requireNonNull(serverEntity);
    }

    @Override
    public void registerComponent(ComponentStanzaProcessorFactory processorFactory, Component component) {
        ComponentStanzaProcessor processor = processorFactory.build();
        Entity fullDomain = EntityUtils.createComponentDomain(component.getSubdomain(), serverEntity);
        component.getComponentHandlers(fullDomain).forEach(processor::addHandler);
        component.getComponentHandlerDictionnaries(fullDomain).forEach(processor::addDictionary);
        processorBySubdomain.put(component.getSubdomain(), processor);
    }

    @Override
    public StanzaProcessor getComponentStanzaProcessor(Entity entity) {
        String serverDomain = serverEntity.getDomain();
        if (!EntityUtils.isAddressingServerComponent(entity, serverEntity)) {
            return null;
        }
        String domain = entity.getDomain();
        String subdomain = domain.replace("." + serverDomain, "");
        return processorBySubdomain.get(subdomain);
    }

}
