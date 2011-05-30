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
package org.apache.vysper.xmpp.authentication;

import org.apache.vysper.xmpp.modules.core.sasl.SASLFailureType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * responses used during SASL authentication
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class AuthenticationResponses {

    public Stanza getSuccess() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("success", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL);
        return stanzaBuilder.build();
    }

    public Stanza getFailureNotAuthorized() {
        return getFailure(SASLFailureType.NOT_AUTHORIZED);
    }

    public Stanza getFailure(SASLFailureType failureType) {
        return new StanzaBuilder("failure", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).startInnerElement(
                failureType.value(), NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).endInnerElement().build();
    }

    public Stanza getFailureMalformedRequest() {
        return getFailure(SASLFailureType.MALFORMED_REQUEST);
    }
}
