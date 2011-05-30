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

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.FINISHED;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.uuid.JVMBuiltinUUIDGenerator;
import org.apache.vysper.xmpp.uuid.UUIDGenerator;

/**
 * handles SASL ANONYMOUS mechanism, where no credentials are required
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "rfc4505", status = FINISHED, coverage = COMPLETE)
public class Anonymous implements SASLMechanism {

    UUIDGenerator uuidGenerator = new JVMBuiltinUUIDGenerator();

    public String getName() {
        return "ANONYMOUS";
    }

    public Stanza started(SessionContext sessionContext, SessionStateHolder sessionStateHolder, Stanza authStanza) {

        // assign a self-created node name
        EntityImpl initiatingEntity = new EntityImpl(uuidGenerator.create(), sessionContext.getServerJID().getDomain(),
                null);
        sessionContext.setInitiatingEntity(initiatingEntity);

        sessionStateHolder.setState(SessionState.AUTHENTICATED);
        return new AuthenticationResponses().getSuccess();
    }

}
