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
package org.apache.vysper.xmpp.protocol;

/**
 * stream-level error conditions
 * as of: RFC3920 4.7.3.  Defined Conditions
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public enum StreamErrorCondition {

    BAD_FORMAT("bad-format"), BAD_NAMESPACE_PREFIX("bad-namespace-prefix"), CONFLICT("conflict"), CONNECTION_TIMEOUT(
            "connection-timeout"), HOST_GONE("host-gone"), HOST_UNKNOWN("host-unknown"), IMPROPER_ADDRESSING(
            "improper-addressing"), INTERNAL_SERVER_ERROR("internal-server-error"), INVALID_FROM("invalid-from"), INVALID_ID(
            "invalid-id"), INVALID_NAMESPACE("invalid-namespace"), INVALID_XML("invalid-xml"), NOT_AUTHORIZED(
            "not-authorized"), POLICY_VIOLATION("policy-violation"), REMOTE_CONNECTION_FAILED(
            "remote-connection-failed"), RESOURCE_CONSTRAINT("resource-constraint"), RESTRICTED_XML("restricted-xml"), SEE_OTHER_HOST(
            "see-other-host"), SYSTEM_SHUTDOWN("system-shutdown"), UNDEFINED_CONDITION("undefined-condition"), UNSUPPORTED_ENCODING(
            "unsupported-encoding"), UNSUPPORTED_STANZA_TYPE("unsupported-stanza-type"), UNSUPPORTED_VERSION(
            "unsupported-version"), XML_NOT_WELL_FORMED("xml-not-well-formed");

    private final String value;

    StreamErrorCondition(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
