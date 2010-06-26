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
package org.apache.vysper.xmpp.modules.roster;

import static org.apache.vysper.xmpp.modules.roster.AskSubscriptionType.ASK_SUBSCRIBE;
import static org.apache.vysper.xmpp.modules.roster.AskSubscriptionType.ASK_SUBSCRIBED;
import static org.apache.vysper.xmpp.modules.roster.AskSubscriptionType.NOT_SET;
import static org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator.Result.ALREADY_SET;
import static org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator.Result.FAILED;
import static org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator.Result.ILLEGAL_ARGUMENT;
import static org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator.Result.OK;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.BOTH;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.FROM;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.NONE;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.REMOVE;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.TO;
import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;

/**
 */
public class RosterSubscriptionMutatorTestCase extends TestCase {

    @Override
    public void setUp() {
        // Add your code here
    }

    public void testAddSubscriptionRequest() {
        checkAdd(NONE, NOT_SET, NOT_SET, ILLEGAL_ARGUMENT, null, null);

        // most simple cases
        checkAdd(NONE, NOT_SET, ASK_SUBSCRIBE, OK, NONE, ASK_SUBSCRIBE);
        checkAdd(NONE, NOT_SET, ASK_SUBSCRIBED, OK, NONE, ASK_SUBSCRIBED);

        // one existing subscription, 
        checkAdd(TO, NOT_SET, ASK_SUBSCRIBED, OK, TO, ASK_SUBSCRIBED);
        checkAdd(FROM, NOT_SET, ASK_SUBSCRIBE, OK, FROM, ASK_SUBSCRIBE);

        // status already set
        checkAdd(TO, ASK_SUBSCRIBED, ASK_SUBSCRIBED, OK, null, null);
        checkAdd(FROM, ASK_SUBSCRIBE, ASK_SUBSCRIBE, OK, null, null);
        checkAdd(TO, NOT_SET, ASK_SUBSCRIBE, ALREADY_SET, null, null);
        checkAdd(FROM, NOT_SET, ASK_SUBSCRIBED, ALREADY_SET, null, null);

        // BOTH + pending is kind of illegal state. well anyway...
        checkAdd(BOTH, NOT_SET, ASK_SUBSCRIBED, ALREADY_SET, null, null);
        checkAdd(BOTH, NOT_SET, ASK_SUBSCRIBE, ALREADY_SET, null, null);
        checkAdd(BOTH, ASK_SUBSCRIBED, ASK_SUBSCRIBED, ALREADY_SET, null, null);
        checkAdd(BOTH, ASK_SUBSCRIBE, ASK_SUBSCRIBE, ALREADY_SET, null, null);

        // special cases for conflicting SUBSCRIBE/SUBSCRIBED stati
        checkAdd(NONE, ASK_SUBSCRIBED, ASK_SUBSCRIBE, OK, NONE, ASK_SUBSCRIBE);
        checkAdd(NONE, ASK_SUBSCRIBE, ASK_SUBSCRIBED, FAILED, null, null);

    }

    public void testAddSubscription() {
        checkAdd(NONE, NOT_SET, NONE, ILLEGAL_ARGUMENT, null, null);
        checkAdd(BOTH, NOT_SET, NONE, ILLEGAL_ARGUMENT, null, null);
        checkAdd(REMOVE, NOT_SET, NONE, ILLEGAL_ARGUMENT, null, null);

        checkAdd(TO, NOT_SET, TO, ALREADY_SET, null, null);
        checkAdd(FROM, NOT_SET, FROM, ALREADY_SET, null, null);

        checkAdd(NONE, NOT_SET, FROM, OK, FROM, NOT_SET);
        checkAdd(NONE, ASK_SUBSCRIBED, FROM, OK, FROM, NOT_SET);
        checkAdd(NONE, ASK_SUBSCRIBE, FROM, OK, FROM, ASK_SUBSCRIBE);

        checkAdd(NONE, NOT_SET, TO, OK, TO, NOT_SET);
        checkAdd(NONE, ASK_SUBSCRIBE, TO, OK, TO, NOT_SET);
        checkAdd(NONE, ASK_SUBSCRIBED, TO, OK, TO, ASK_SUBSCRIBED);

        checkAdd(TO, NOT_SET, FROM, OK, BOTH, NOT_SET);
        checkAdd(TO, ASK_SUBSCRIBED, FROM, OK, BOTH, NOT_SET);
        checkAdd(TO, ASK_SUBSCRIBE, FROM, OK, BOTH, ASK_SUBSCRIBE);

        checkAdd(FROM, NOT_SET, TO, OK, BOTH, NOT_SET);
        checkAdd(FROM, ASK_SUBSCRIBE, TO, OK, BOTH, NOT_SET);
        checkAdd(FROM, ASK_SUBSCRIBED, TO, OK, BOTH, ASK_SUBSCRIBED);

    }

    public void testRemoveSubscription() {
        // TODO Add your code here
    }

    private void checkAdd(SubscriptionType initialSubscriptionType, AskSubscriptionType initialAskSubscriptionType,
            SubscriptionType parameterSubscriptionType, RosterSubscriptionMutator.Result expectedResult,
            SubscriptionType expectedSubscriptionType, AskSubscriptionType expectedAskSubscriptionType) {

        RosterItem item = prepareItem(initialSubscriptionType, initialAskSubscriptionType);

        // add parameterSubscriptionType 
        RosterSubscriptionMutator.Result subscriptionMutatorResult = new RosterSubscriptionMutator().add(item,
                parameterSubscriptionType);

        checkResult(initialSubscriptionType, initialAskSubscriptionType, expectedResult, expectedSubscriptionType,
                expectedAskSubscriptionType, item, subscriptionMutatorResult);
    }

    private void checkAdd(SubscriptionType initialSubscriptionType, AskSubscriptionType initialAskSubscriptionType,
            AskSubscriptionType parameterAskSubscriptionType, RosterSubscriptionMutator.Result expectedResult,
            SubscriptionType expectedSubscriptionType, AskSubscriptionType expectedAskSubscriptionType) {

        RosterItem item = prepareItem(initialSubscriptionType, initialAskSubscriptionType);

        // add parameterSubscriptionType 
        RosterSubscriptionMutator.Result subscriptionMutatorResult = new RosterSubscriptionMutator().add(item,
                parameterAskSubscriptionType);

        checkResult(initialSubscriptionType, initialAskSubscriptionType, expectedResult, expectedSubscriptionType,
                expectedAskSubscriptionType, item, subscriptionMutatorResult);
    }

    private void checkResult(SubscriptionType initialSubscriptionType, AskSubscriptionType initialAskSubscriptionType,
            RosterSubscriptionMutator.Result expectedResult, SubscriptionType expectedSubscriptionType,
            AskSubscriptionType expectedAskSubscriptionType, RosterItem item,
            RosterSubscriptionMutator.Result subscriptionMutatorResult) {
        assertEquals(expectedResult, subscriptionMutatorResult);

        if (expectedSubscriptionType == null && expectedAskSubscriptionType == null) {
            assertEquals(initialSubscriptionType, item.getSubscriptionType());
            assertEquals(initialAskSubscriptionType, item.getAskSubscriptionType());
        } else {
            assertEquals(expectedSubscriptionType, item.getSubscriptionType());
            assertEquals(expectedAskSubscriptionType, item.getAskSubscriptionType());
        }
    }

    private RosterItem prepareItem(SubscriptionType initialSubscriptionType,
            AskSubscriptionType initialAskSubscriptionType) {
        EntityImpl jid = null;
        try {
            jid = EntityImpl.parse("test@test.org");
        } catch (EntityFormatException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }
        RosterItem item = new RosterItem(jid, "group", initialSubscriptionType, initialAskSubscriptionType);
        return item;
    }
}
