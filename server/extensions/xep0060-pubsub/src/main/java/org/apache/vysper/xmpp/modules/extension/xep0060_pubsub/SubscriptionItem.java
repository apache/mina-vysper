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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import org.apache.vysper.xmpp.addressing.Entity;

/** 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class SubscriptionItem {

    protected String nodeName = null;

    protected String subscriptionID = null;

    protected Entity subscriberJID = null;

    /**
     * Creates a new subscription item with teh supplied name, subscriptionID and subscriber JID.
     */
    public SubscriptionItem(String nodeName, String subID, Entity sub) {
        this.nodeName = nodeName;
        this.subscriptionID = subID;
        this.subscriberJID = sub;
    }

    /**
     * @return the nodeName of the subscription.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @return the JID of the subscriber.
     */
    public Entity getSubscriberJID() {
        return subscriberJID;
    }

    /**
     * @return the subscription ID of this subscription.
     */
    public String getSubscriptionID() {
        return subscriptionID;
    }

    /**
     * @return the state of the subscription. Currently fixed as "subscribed".
     */
    public String getSubscriptionState() {
        return "subscribed"; // TODO
    }

}
