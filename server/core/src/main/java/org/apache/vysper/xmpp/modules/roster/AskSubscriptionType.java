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
 * type of pending subscriptions
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public enum AskSubscriptionType {

    /**
     * no pending subscriptions
     */
    NOT_SET(null),
    /**
     * new subscription request to be confirmed by the contact
     */
    ASK_SUBSCRIBE("subscribe"),
    /**
     * confirmed subscription request
     */
    ASK_SUBSCRIBED("subscribed");

    private final String value;

    AskSubscriptionType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
