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
package org.apache.vysper.xmpp.extension.xep0124;

import junit.framework.TestCase;

/**
 */
public class RequestsWindowTest extends TestCase {

    protected RequestsWindow requestsWindow;

    public void testSetHighestContinousFromDiscontinousInsert() {
        requestsWindow = new RequestsWindow("1");
        queueNewRequest(1L);
        assertHighestContinuous(1);

        queueNewRequest(3L); // 3 before 2
        assertHighestContinuous(1); // HCR is still on 1

        queueNewRequest(2L); // 2 after 3
        assertHighestContinuous(3); // HCR is correct
        
        assertRID(1L, requestsWindow.pollNext());
        assertRID(2L, requestsWindow.pollNext());
        assertRID(3L, requestsWindow.pollNext());
    }
    
    public void testAllowDuplicateRIDs() {
        requestsWindow = new RequestsWindow("1");
        queueNewRequest(100L);
        queueNewRequest(100L); // another 100
        assertEquals(2, requestsWindow.size());
    }

    public void testRequestGap() {
        requestsWindow = new RequestsWindow("1");
        queueNewRequest(3L);
        assertHighestContinuous(3);

        queueNewRequest(5L);
        assertHighestContinuous(3);

        // now, 3 is highest continous, 4 is missing, 5 is highest: [3 HCR, GAP, 5]
        
        // now, fetch 3
        final BoshRequest boshRequest3 = requestsWindow.pollNext();
        assertRID(3L, boshRequest3);
        final BoshRequest boshRequest5 = requestsWindow.pollNext();
        assertHighestContinuous(5);
        System.out.println("reqWin = " + requestsWindow.logRequestWindow());
    }
    
    public void testRequest2Gaps() {
        requestsWindow = new RequestsWindow("1");
        queueNewRequest(3L);
        assertHighestContinuous(3);

        queueNewRequest(5L);
        queueNewRequest(6L);
        assertHighestContinuous(3);

        queueNewRequest(8L);
        queueNewRequest(9L);
        queueNewRequest(10L);
        assertHighestContinuous(3);

        // now we have request [3, 5, 6, 8, 9, 10]
        
        // now, fetch 3
        final BoshRequest boshRequest3 = requestsWindow.pollNext();
        assertRID(3L, boshRequest3);

        assertRID(5L, requestsWindow.pollNext());
        assertRID(6L, requestsWindow.pollNext());
        assertHighestContinuous(6);

        assertRID(8L, requestsWindow.pollNext());
        assertRID(9L, requestsWindow.pollNext());
        assertRID(10L, requestsWindow.pollNext());
        assertHighestContinuous(10);
        
    }

    private void assertRID(final long expectedRid, BoshRequest boshRequest) {
        assertEquals(expectedRid, (long)boshRequest.getRid());
    }

    private void assertHighestContinuous(final int expected) {
        assertEquals(expected, requestsWindow.getHighestContinuousRid());
    }

    private void queueNewRequest(final long rid) {
        requestsWindow.queueRequest(new BoshRequest(null, null, rid));
    }
}
