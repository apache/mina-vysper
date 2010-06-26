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
package org.apache.vysper.xmpp.modules.core.sasl;

/**
 * see also RFC3920/7.5
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public enum SASLFailureType {

    ABORTED("aborted"), // sent as ACK in reply to <abort/>
    INCORRECT_ENCODING("incorrect-encoding"), // BASE64 or other encoding is incorrect
    INVALID_AUTHZID("invalid-authzid"), // RFC 3920: "authzid provided by the initiating entity is invalid,
    // either because it is incorrectly formatted or because the initiating
    // entity does not have permissions to authorize that ID"
    INVALID_MECHANISM("invalid-mechanism"), // this mechanism is not supported by the server
    MALFORMED_REQUEST("malformed-request"), // request is malformed
    MECHANISMS_TOO_WEAK("mechanisms-too-weak"), // RFC 3920: "The mechanism requested by the initiating entity is
    // weaker than server policy permits for that initiating entity"
    NOT_AUTHORIZED("not-authorized"), // sent credentials could not be positively verified
    TEMPORARY_AUTH_FAILURE("temporary-auth-failure"); // try again later!

    private final String value;

    SASLFailureType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}