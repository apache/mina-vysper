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

/**
 * see http://www.xmpp.org/internet-drafts/draft-saintandre-rfc3921bis-05.html#roster-syntax-subscription
 * "none" -- the user does not have a subscription to the contact's presence, and the contact does
 *    not have a subscription to the user's presence
 * "to" -- the user has a subscription to the contact's presence, but the contact does not have a
 *    subscription to the user's presence
 * "from" -- the contact has a subscription to the user's presence, but the user does not have a
 *    subscription to the contact's presence
 * "both" -- both the user and the contact have subscriptions to each other's presence (also called
 *    a "mutual subscription")
 *
 * remove is a special case:
 * In a roster set, the value of the 'subscription' attribute MAY be "remove", which indicates that the item is to be
 * removed from the roster; a receiving server MUST ignore all values of the 'subscription' attribute other than "remove".
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public enum SubscriptionType {

    BOTH("both"), FROM("from"), NONE("none"), REMOVE("remove"), TO("to");

    private final String value;

    SubscriptionType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean includesFrom() {
        return this == FROM || this == BOTH;
    }

    public boolean includesTo() {
        return this == TO || this == BOTH;
    }

    public boolean acceptsTo() {
        return this == NONE || this == FROM;
    }

    public boolean acceptsFrom() {
        return this == NONE || this == TO;
    }

    public static SubscriptionType addState(SubscriptionType old, SubscriptionType add) {
        switch (add) {

        case BOTH:
            throw new RuntimeException("add 'both' not valid");

        case FROM:
            if (!old.acceptsFrom())
                throw new RuntimeException("cannot add " + add.value() + " to " + old.value());
            if (old == NONE)
                return FROM;
            if (old == TO)
                return BOTH;
            throw new RuntimeException("add FROM not supported for " + old.value());

        case NONE:
            return add;

        case REMOVE:
            throw new RuntimeException("add 'remove' not valid");

        case TO:
            if (!old.acceptsTo())
                throw new RuntimeException("cannot add " + add.value() + " to " + old.value());
            if (old == NONE)
                return TO;
            if (old == FROM)
                return BOTH;
            throw new RuntimeException("add TO not supported for " + old.value());

        default:
            throw new RuntimeException("not implemented: adding " + add.value());

        }
    }

}
