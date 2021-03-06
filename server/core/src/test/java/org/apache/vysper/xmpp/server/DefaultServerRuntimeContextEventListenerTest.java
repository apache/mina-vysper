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
package org.apache.vysper.xmpp.server;

import java.util.Optional;

import org.apache.vysper.event.EventListenerDictionary;
import org.apache.vysper.event.EventListenerMock;
import org.apache.vysper.event.SimpleEventListenerDictionary;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.DefaultModule;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * @author Réda Housni Alaoui
 */
public class DefaultServerRuntimeContextEventListenerTest {

    private EventListenerMock<Event> eventListener;

    private DefaultServerRuntimeContext tested;

    @Before
    public void before() {
        eventListener = new EventListenerMock<>();
        EventListenerDictionary listenerDictionary = SimpleEventListenerDictionary.builder()
                .register(Event.class, eventListener).build();

		tested = new DefaultServerRuntimeContext(mock(Entity.class), mock(StanzaRelay.class));
        tested.addModule(new MyModule(listenerDictionary));
    }

    @Test
    public void publishEventGivenRegisteredModule() {
        Event event = new Event();
        tested.getEventBus().publish(Event.class, event);
        eventListener.assertReceivedEventsSequence(event);
    }

    private static class Event {

    }

    private static class MyModule extends DefaultModule {

        private final EventListenerDictionary eventListenerDictionary;

        private MyModule(EventListenerDictionary eventListenerDictionary) {
            this.eventListenerDictionary = eventListenerDictionary;
        }

        @Override
        public String getName() {
            return "MyModule";
        }

        @Override
        public String getVersion() {
            return "1.0";
        }

        @Override
        public Optional<EventListenerDictionary> getEventListenerDictionary() {
            return Optional.of(eventListenerDictionary);
        }
    }
}