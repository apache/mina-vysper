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
package org.apache.vysper.event;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleEventBus implements EventBus {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleEventBus.class);

    private final Set<EventListenerDictionary> listenerDictionaries = new LinkedHashSet<>();

    public SimpleEventBus addDictionary(EventListenerDictionary dictionary) {
        listenerDictionaries.add(dictionary);
        return this;
    }

    @Override
    public <T> EventBus publish(Class<T> eventType, T event) {
        listenerDictionaries.stream().map(eventListenerDictionary -> eventListenerDictionary.get(eventType))
                .flatMap(Collection::stream).distinct().forEach(listener -> fireEvent(event, listener));
        return this;
    }

    @SuppressWarnings("unchecked")
    private <T> void fireEvent(T event, EventListener<?> listener) {
        LOG.trace("Firing event {} on listener {}", event, listener);
        try {
            ((EventListener<T>) listener).onEvent(event);
            LOG.trace("Fired event {} on listener {}", event, listener);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
