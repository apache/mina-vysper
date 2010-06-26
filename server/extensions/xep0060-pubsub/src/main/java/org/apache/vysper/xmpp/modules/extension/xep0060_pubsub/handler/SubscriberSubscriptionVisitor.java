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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriberVisitor;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.SubscriptionItem;

/**
 * This Visitor is used to traverse over all subscriptions of a user (matching bare JIDs).
 *  
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class SubscriberSubscriptionVisitor implements SubscriberVisitor {

    // the collected subscriptions
    protected List<SubscriptionItem> subscriptions = null;

    // the user to filter
    protected Entity bareJID = null;

    /**
     * Create a new visitor with the user as filter.
     */
    public SubscriberSubscriptionVisitor(Entity userJID) {
        this.bareJID = userJID.getBareJID();
        subscriptions = new ArrayList<SubscriptionItem>();
    }

    /**
     * Traverses all subscriptions and collects the subscriptions of the
     * user (matching bareJID).
     */
    public void visit(String nodeName, String subID, Entity sub) {
        if (sub.getBareJID().equals(bareJID)) {
            SubscriptionItem si = new SubscriptionItem(nodeName, subID, sub);
            subscriptions.add(si);
        }
    }

    /**
     * Returns the list of subscriptions.
     */
    public List<SubscriptionItem> getSubscriptions() {
        return subscriptions;
    }
}
