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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;

/**
 * This class handles the configure node part of the XEP-0060 specification. This means
 * all stanzas with "configure" within the pubsub#owner namespace.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", section = "8.2", status = SpecCompliant.ComplianceStatus.NOT_STARTED, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public class PubSubOwnerConfigureNodeHandler extends AbstractPubSubOwnerHandler {

    /**
     * Create a new configuration handler with the supplied configuration object.
     */
    public PubSubOwnerConfigureNodeHandler(PubSubServiceConfiguration serviceConfiguration) {
        super(serviceConfiguration);
    }

    /**
     * @return "configure" as worker element.
     */
    @Override
    protected String getWorkerElement() {
        return "configure";
    }

}
