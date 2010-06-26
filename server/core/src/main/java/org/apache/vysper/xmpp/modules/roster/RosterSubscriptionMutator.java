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
import static org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator.Result.OK;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.BOTH;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.FROM;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.NONE;
import static org.apache.vysper.xmpp.modules.roster.SubscriptionType.TO;

/**
 * changes roster item subscription and ask states according to the protocol's spec
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class RosterSubscriptionMutator {

    private static RosterSubscriptionMutator SINGLETON = new RosterSubscriptionMutator();

    public static RosterSubscriptionMutator getInstance() {
        return SINGLETON;
    }

    public enum Result {
        /** the type was added to the subscription state**/
        OK,
        /** an illegal type was supplied **/
        ILLEGAL_ARGUMENT,
        /** the type was already present **/
        ALREADY_SET,
        /** the type was not added **/
        FAILED
    }

    protected RosterSubscriptionMutator() {
        ; // empty
    }

    /**
     * adds a subscription request to the roster item
     */
    public Result add(RosterItem item, AskSubscriptionType addAskSubscriptionType) {
        synchronized (item) {
            return addWorker(item, addAskSubscriptionType);
        }
    }

    /**
     * adds a subscription to the roster item
     */
    public Result add(RosterItem item, SubscriptionType addSubscriptionType) {
        synchronized (item) {
            return addWorker(item, addSubscriptionType);
        }
    }

    /**
     * removes a subscription from the roster item
     */
    public Result remove(RosterItem item, SubscriptionType removeSubscriptionType) {
        synchronized (item) {
            return removeWorker(item, removeSubscriptionType);
        }
    }

    protected Result addWorker(RosterItem item, SubscriptionType addSubscriptionType) {
        switch (addSubscriptionType) {
        case NONE:
        case BOTH:
        case REMOVE:
            return Result.ILLEGAL_ARGUMENT;
        case FROM:
            return addFrom(item);
        case TO:
            return addTo(item);
        default:
            throw new IllegalArgumentException("unhandled SubscriptionType " + addSubscriptionType.value());
        }
    }

    protected Result addWorker(RosterItem item, AskSubscriptionType addAskSubscriptionType) {
        switch (addAskSubscriptionType) {
        case NOT_SET:
            return Result.ILLEGAL_ARGUMENT;
        case ASK_SUBSCRIBE:
            return addAskSubscribe(item);
        case ASK_SUBSCRIBED:
            return addAskSubscribed(item);
        default:
            throw new IllegalArgumentException("unhandled SubscriptionType " + addAskSubscriptionType.value());
        }
    }

    protected Result addAskSubscribe(RosterItem item) {
        SubscriptionType type = item.getSubscriptionType();
        AskSubscriptionType typeAsk = item.getAskSubscriptionType();
        if (type.includesTo())
            return ALREADY_SET;
        if (typeAsk == ASK_SUBSCRIBE)
            return OK;
        // IGNORE this, overwrite! if (typeAsk == ASK_SUBSCRIBED) return ALREADY_SET;
        item.setAskSubscriptionType(ASK_SUBSCRIBE);
        return OK;
    }

    protected Result addAskSubscribed(RosterItem item) {
        SubscriptionType type = item.getSubscriptionType();
        AskSubscriptionType typeAsk = item.getAskSubscriptionType();
        if (type.includesFrom())
            return ALREADY_SET;
        if (typeAsk == ASK_SUBSCRIBE)
            return FAILED; // TODO think about return value
        if (typeAsk == ASK_SUBSCRIBED)
            return OK;
        item.setAskSubscriptionType(ASK_SUBSCRIBED);
        return OK;
    }

    protected Result addTo(RosterItem item) {
        SubscriptionType type = item.getSubscriptionType();
        if (type.includesTo())
            return ALREADY_SET;
        if (type == NONE) {
            type = TO;
        } else if (type == FROM) {
            type = BOTH;
        }
        item.setSubscriptionType(type);
        if (item.getAskSubscriptionType() == ASK_SUBSCRIBE)
            item.setAskSubscriptionType(NOT_SET);
        return OK;
    }

    protected Result addFrom(RosterItem item) {
        SubscriptionType type = item.getSubscriptionType();
        if (type.includesFrom())
            return ALREADY_SET;
        if (type == NONE) {
            type = FROM;
        } else if (type == TO) {
            type = BOTH;
        }
        item.setSubscriptionType(type);
        if (item.getAskSubscriptionType() == ASK_SUBSCRIBED)
            item.setAskSubscriptionType(NOT_SET);
        return OK;
    }

    protected Result removeWorker(RosterItem item, SubscriptionType removeSubscriptionType) {
        switch (removeSubscriptionType) {
        case NONE:
        case BOTH:
        case REMOVE:
            return Result.ILLEGAL_ARGUMENT;
        case FROM:
            return removeFrom(item);
        case TO:
            return removeTo(item);
        default:
            throw new IllegalArgumentException("unhandled SubscriptionType " + removeSubscriptionType.value());
        }
    }

    protected Result removeTo(RosterItem item) {
        SubscriptionType type = item.getSubscriptionType();
        if (!type.includesTo()) {
            // if sub was asked, remove that.
            AskSubscriptionType askType = item.getAskSubscriptionType();
            if (askType != ASK_SUBSCRIBE)
                return ALREADY_SET;
            item.setAskSubscriptionType(NOT_SET);
            return OK;
        }
        if (type == BOTH) {
            type = FROM;
        } else if (type == TO) {
            type = NONE;
        }
        item.setSubscriptionType(type);
        return OK;
    }

    protected Result removeFrom(RosterItem item) {
        SubscriptionType type = item.getSubscriptionType();
        if (!type.includesFrom()) {
            // if sub was asked, remove that.
            AskSubscriptionType askType = item.getAskSubscriptionType();
            if (askType != ASK_SUBSCRIBED)
                return ALREADY_SET;
            item.setAskSubscriptionType(NOT_SET);
            return OK;
        }
        if (type == BOTH) {
            type = TO;
        } else if (type == FROM) {
            type = NONE;
        }
        item.setSubscriptionType(type);
        return OK;
    }
}
