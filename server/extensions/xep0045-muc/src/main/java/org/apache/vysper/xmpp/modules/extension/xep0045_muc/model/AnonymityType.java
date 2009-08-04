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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import org.apache.vysper.compliance.SpecCompliant;

/**
 * Describes the different anonymity settings for a {@link Room}
 * @author The Apache MINA Project (dev@mina.apache.org)
 *
 */
@SpecCompliant(spec="xep-0045", section="4.2", status= SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public enum AnonymityType {

    /**
     * A room in which the full JIDs or bare JIDs of occupants cannot be discovered by anyone, including room admins and room owners; such rooms are NOT RECOMMENDED or explicitly supported by MUC, but are possible using this protocol if a service implementation offers the appropriate configuration options; contrast with Non-Anonymous Room and Semi-Anonymous Room.
     */
    FullyAnonymous,

    /**
     * A room in which an occupant's full JID is exposed to all other occupants, although the occupant may choose any desired room nickname; contrast with Semi-Anonymous Room and Fully-Anonymous Room.
     */
    NonAnonymous,

    /**
     * A room in which an occupant's full JID can be discovered by room admins only; contrast with Fully-Anonymous Room and Non-Anonymous Room.
     */
    SemiAnonymous,
}
