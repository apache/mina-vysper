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
package org.apache.vysper.xmpp.delivery.inbound;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;

/**
 */
public class RelayResultTestCase extends TestCase {

    public void testDefaultCstr() {
        RelayResult relayResult = new RelayResult();
        Assert.assertFalse(relayResult.hasProcessingErrors());
        Assert.assertNotNull(relayResult.getProcessingErrors());
        Assert.assertEquals(0, relayResult.getProcessingErrors().size());
    }

    public void testCstrDeliveryException() {
        RelayResult relayResult = new RelayResult(new DeliveryException());
        Assert.assertTrue(relayResult.hasProcessingErrors());
        Assert.assertEquals(1, relayResult.getProcessingErrors().size());
    }

    public void testAddProcessingError() {
        RelayResult relayResult = new RelayResult(new DeliveryException());
        relayResult.addProcessingError(new DeliveryException());
        
        Assert.assertTrue(relayResult.hasProcessingErrors());
        Assert.assertEquals(2, relayResult.getProcessingErrors().size());
    }

}
