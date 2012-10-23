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
    }

    public void testRequestGap() {
        requestsWindow = new RequestsWindow("1");
        queueNewRequest(3L);
        assertHighestContinuous(3);

        queueNewRequest(5L);
        assertHighestContinuous(3);

        // now, 3 is highest continous, 4 is missing, 5 is highest: [3 HCR, GAP, 5]
        
        final BoshRequest boshRequest = requestsWindow.pollNext();
        assertHighestContinuous(3); // BUG! should go to 5
        System.out.println("reqWin = " + requestsWindow.logRequestWindow()); // HCR is not in line
    }

    private void assertHighestContinuous(final int expected) {
        assertEquals(expected, requestsWindow.getHighestContinuousRid());
    }

    private void queueNewRequest(final long rid) {
        requestsWindow.queueRequest(new BoshRequest(null, null, rid));
    }
}
