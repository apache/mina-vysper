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

import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleEventListenerDictionary implements EventListenerDictionary {

    private final Map<Class<?>, Set<EventListener<?>>> listenersByEventType;

    private SimpleEventListenerDictionary(Map<Class<?>, Set<EventListener<?>>> listenersByEventType) {
        this.listenersByEventType = new HashMap<>(listenersByEventType);
    }

    @Override
    public Set<EventListener<?>> get(Class<?> eventType) {
        return ofNullable(listenersByEventType.get(eventType)).orElse(Collections.emptySet());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<Class<?>, Set<EventListener<?>>> listenersByEventType = new HashMap<>();

        private Builder() {
        }

        /**
         * Register the provided listener to the provided event type. Since the listener
         * reference will never be released, be aware of possible memory leak.
         */
        public <T> Builder register(Class<T> eventType, EventListener<T> listener) {
            listenersByEventType.computeIfAbsent(eventType, type -> new LinkedHashSet<>()).add(listener);
            return this;
        }

        public SimpleEventListenerDictionary build() {
            return new SimpleEventListenerDictionary(listenersByEventType);
        }
    }

}
