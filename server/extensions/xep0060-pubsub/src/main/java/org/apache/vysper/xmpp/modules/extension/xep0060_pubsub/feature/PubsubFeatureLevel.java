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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.feature;

import org.apache.vysper.compliance.SpecCompliant;

/**
 * Specifies which level of compliance a certain pubsub feature is in.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public enum PubsubFeatureLevel {
    REQUIRED, RECOMMENDED, OPTIONAL
}
