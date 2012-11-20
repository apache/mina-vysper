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

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.PARTIAL;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.IN_PROGRESS;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.sasl.SASLFailureType;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * handles SASL PLAIN mechanism. this mechanism is standardized in RFC4616
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliance(compliant = {
        @SpecCompliant(spec = "rfc4616", status = IN_PROGRESS, coverage = PARTIAL),
        @SpecCompliant(spec = "rfc3920bis-09", section = "15.6.", status = IN_PROGRESS, coverage = PARTIAL, comment = "PLAIN is mandatory now") })
public class Plain implements SASLMechanism {

    private static final AuthenticationResponses AUTHENTICATION_RESPONSES = new AuthenticationResponses();
    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    public String getName() {
        return "PLAIN";
    }

    public Stanza started(SessionContext sessionContext, SessionStateHolder sessionStateHolder, Stanza authStanza) {
        // TODO assure, that connection is secured via TLS. if not, reject SASL PLAIN

        List<XMLText> innerTexts = authStanza.getInnerTexts();
        if (innerTexts == null || innerTexts.isEmpty())
            return AUTHENTICATION_RESPONSES.getFailureMalformedRequest();

        // retrieve credential payload and decode from BASE64
        XMLText base64Encoded = innerTexts.get(0);
        byte[] decoded;
        try {
            decoded = Base64.decodeBase64(base64Encoded.getText().getBytes(CHARSET_UTF8));
        } catch (Throwable e) {
            return AUTHENTICATION_RESPONSES.getFailure(SASLFailureType.INCORRECT_ENCODING);
        }

        // parse clear text, extract parts, which are separated by zeros
        List<String> decodedParts = new ArrayList<String>();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < decoded.length; i++) {
            char ch = (char) decoded[i];
            if (ch != 0) {
                stringBuilder.append(ch);
            }
            if (ch == 0 || i == decoded.length - 1) {
                decodedParts.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            }
        }

        if (decodedParts.size() != 3) {
            return AUTHENTICATION_RESPONSES.getFailureMalformedRequest();
        }

        String alias = decodedParts.get(0); // "authorization identity (identity to act as)", currently unused
        String username = decodedParts.get(1); // "authentication identity (identity whose password will be used)"
        String password = decodedParts.get(2);

        if (!username.contains("@"))
            username = username + "@" + sessionContext.getServerJID().getDomain();
        EntityImpl initiatingEntity;
        try {
            initiatingEntity = EntityImpl.parse(username);
        } catch (EntityFormatException e) {
            return AUTHENTICATION_RESPONSES.getFailureNotAuthorized();
        }

        boolean authorized = sessionContext.getServerRuntimeContext().getUserAuthentication().verifyCredentials(
        		initiatingEntity, password, null);

        if (authorized) {
            sessionContext.setInitiatingEntity(initiatingEntity);
            sessionStateHolder.setState(SessionState.AUTHENTICATED);
            return AUTHENTICATION_RESPONSES.getSuccess();
        } else {
            return AUTHENTICATION_RESPONSES.getFailureNotAuthorized();
        }
    }
}
