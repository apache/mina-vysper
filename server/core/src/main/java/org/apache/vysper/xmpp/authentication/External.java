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
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.NOT_STARTED;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * handles SASL EXTERNAL mechanism
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliance(compliant = {
        @SpecCompliant(spec = "rfc4422", section = "A.", status = NOT_STARTED, coverage = PARTIAL, comment = "only Appendix A. is relevant here"),
        @SpecCompliant(spec = "rfc3920bis-09", section = "15.6.", status = IN_PROGRESS, coverage = PARTIAL, comment = "EXTERNAL is mandatory") })
public class External implements SASLMechanism {
    public String getName() {
        return "EXTERNAL";
    }

    public Stanza started(SessionContext sessionContext, SessionStateHolder sessionStateHolder, Stanza authStanza) {
        throw new RuntimeException("not yet implemented");
    }
}
