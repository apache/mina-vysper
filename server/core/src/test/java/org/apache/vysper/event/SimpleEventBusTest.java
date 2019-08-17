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

import org.junit.Before;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleEventBusTest {

    private EventListenerMock<FooEvent> fooListener;

    private EventListenerMock<BarEvent> barListener;

    private EventListenerMock<BazEvent> bazListener;

    private SimpleEventBus tested;

    @Before
    public void before() {
        tested = new SimpleEventBus();

        fooListener = new EventListenerMock<>();
        barListener = new EventListenerMock<>();
        bazListener = new EventListenerMock<>();
    }

    @Test
    public void publishEventGivenOneDictionary() {
        tested.addDictionary(SimpleEventListenerDictionary.builder().register(FooEvent.class, fooListener)
                .register(BarEvent.class, barListener).register(BazEvent.class, bazListener).build());

        FooEvent fooEvent = new FooEvent();
        tested.publish(FooEvent.class, fooEvent);
        BarEvent barEvent = new BarEvent();
        tested.publish(BarEvent.class, barEvent);
        BazEvent bazEvent = new BazEvent();
        tested.publish(BazEvent.class, bazEvent);

        fooListener.assertReceivedEventsSequence(fooEvent);
        barListener.assertReceivedEventsSequence(barEvent);
        bazListener.assertReceivedEventsSequence(bazEvent);
    }

    @Test
    public void publishEventGivenTwoDictionaries() {
        tested.addDictionary(SimpleEventListenerDictionary.builder().register(FooEvent.class, fooListener)
                .register(BarEvent.class, barListener).build())
                .addDictionary(SimpleEventListenerDictionary.builder().register(BazEvent.class, bazListener).build());

        FooEvent fooEvent = new FooEvent();
        tested.publish(FooEvent.class, fooEvent);
        BarEvent barEvent = new BarEvent();
        tested.publish(BarEvent.class, barEvent);
        BazEvent bazEvent = new BazEvent();
        tested.publish(BazEvent.class, bazEvent);

        fooListener.assertReceivedEventsSequence(fooEvent);
        barListener.assertReceivedEventsSequence(barEvent);
        bazListener.assertReceivedEventsSequence(bazEvent);
    }

    @Test
    public void publishEventGivenListenerRegisteredTwiceInOneDictionary() {
        tested.addDictionary(SimpleEventListenerDictionary.builder().register(FooEvent.class, fooListener)
                .register(FooEvent.class, fooListener).build());

        FooEvent fooEvent = new FooEvent();
        tested.publish(FooEvent.class, fooEvent);
        fooListener.assertReceivedEventsSequence(fooEvent);
    }

    @Test
    public void publishEventGivenListenerRegisteredTwiceInTwoDifferentDictionary() {
        tested.addDictionary(SimpleEventListenerDictionary.builder().register(FooEvent.class, fooListener).build())
                .addDictionary(SimpleEventListenerDictionary.builder().register(FooEvent.class, fooListener).build());

        FooEvent event = new FooEvent();
        tested.publish(FooEvent.class, event);
        fooListener.assertReceivedEventsSequence(event);
    }

    @Test
    public void publishSubClassEvent() {
        tested.addDictionary(SimpleEventListenerDictionary.builder().register(FooEvent.class, fooListener).build());

        SubFooEvent event = new SubFooEvent();
        tested.publish(FooEvent.class, event);
        fooListener.assertReceivedEventsSequence(event);
    }

    @Test
    public void publishOnTwoListenersGivenOneFailingListener() {
        EventListenerMock<FooEvent> failingListener = new EventListenerMock<FooEvent>().failOnReceivedEvent();
        tested.addDictionary(SimpleEventListenerDictionary.builder().register(FooEvent.class, failingListener)
                .register(FooEvent.class, fooListener).build());

        FooEvent event = new FooEvent();
        tested.publish(FooEvent.class, event);

        failingListener.assertReceivedEventsSequence(event);
        fooListener.assertReceivedEventsSequence(event);
    }

    private static class FooEvent {

    }

    private static class BarEvent {

    }

    private static class BazEvent {

    }

    private static class SubFooEvent extends FooEvent {

    }

}