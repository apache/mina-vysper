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

package org.apache.vysper.xmpp.stanza;

/**
 * inner error for stanzas of type iq, message, presence
 * as of: RFC3920 9.3.3. Defined Conditions
 * There are also recommendations for the associated error type
 * to be found.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public enum StanzaErrorCondition {

    BAD_REQUEST("bad-request"), CONFLICT("conflict"), FEATURE_NOT_IMPLEMENTED("feature-not-implemented"), FORBIDDEN(
            "forbidden"), GONE("gone"), INTERNAL_SERVER_ERROR("internal-server-error"), ITEM_NOT_FOUND("item-not-found"), JID_MALFORMED(
            "jid-malformed"), NOT_ACCEPTABLE("not-acceptable"), NOT_ALLOWED("not-allowed"), NOT_AUTHORIZED(
            "not-authorized"), NOT_MODIFIED("not-modified"), PAYMENT_REQUIRED("payment-required"), RECIPIENT_UNAVAILABLE(
            "recipient-unavailable"), REDIRECT("redirect"), REGISTRATION_REQUIRED("registration-required"), REMOTE_SERVER_NOT_FOUND(
            "remote-server-not-found"), REMOTE_SERVER_TIMEOUT("remote-server-timeout"), RESOURCE_CONSTRAINT(
            "resource-constraint"), SERVICE_UNAVAILABLE("service-unavailable"), SUBSCRIPTION_REQUIRED(
            "subscription-required"), UNDEFINED_CONDITION("undefined-condition"), UNEXPECTED_REQUEST(
            "unexpected-request"), UNKNOWN_SENDER("unknown-sender");

    private final String value;

    StanzaErrorCondition(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
