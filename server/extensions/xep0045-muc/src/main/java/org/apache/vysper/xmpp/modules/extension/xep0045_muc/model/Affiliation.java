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
 * These affiliations are long-lived in that they persist across a user's visits to the room and 
 * are not affected by happenings in the room. In addition, there is no one-to-one mapping between 
 * these affiliations and an occupant's role within the room. Affiliations are granted, revoked, 
 * and maintained based on the user's bare JID.
 * 
 * If a user without a defined affiliation enters a room, the user's affiliation is defined as "none"; 
 * however, this affiliation does not persist across visits (i.e., a service does not maintain a "none 
 * list" across visits).
 * 
 * The member affiliation provides a way for a room owner or admin to specify a "whitelist" of users 
 * who are allowed to enter a members-only room. When a member enters a members-only room, his or her 
 * affiliation does not change, no matter what his or her role is. The member affiliation also provides 
 * a way for users to effectively register with an open room and thus be lastingly associated with that 
 * room in some way (one result may be that the user's nickname is reserved in the room).
 * 
 * An outcast is a user who has been banned from a room and who is not allowed to enter the room.
 * 
 * Information about affiliations MUST be sent in all presence stanzas generated or reflected by the room 
 * and sent to occupants
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0045", section = "5.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public enum Affiliation {

    Owner, Admin, Member, None, Outcast;

    public static Affiliation fromString(String s) {
        if ("owner".equals(s))
            return Owner;
        else if ("admin".equals(s))
            return Admin;
        else if ("member".equals(s))
            return Member;
        else if ("outcast".equals(s))
            return Outcast;
        else if ("none".equals(s))
            return None;
        else
            throw new IllegalArgumentException("Unknown affiliation: " + s);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
