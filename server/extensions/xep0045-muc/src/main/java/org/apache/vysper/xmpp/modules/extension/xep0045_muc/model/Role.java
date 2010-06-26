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

import java.util.EnumSet;

import org.apache.vysper.compliance.SpecCompliant;

/**
 * Roles are temporary in that they do not necessarily persist across a user's visits to the room and MAY 
 * change during the course of an occupant's visit to the room. An implementation MAY persist roles across 
 * visits and SHOULD do so for moderated rooms (since the distinction between visitor and participant is 
 * critical to the functioning of a moderated room).
 * 
 * There is no one-to-one mapping between roles and affiliations (e.g., a member could be a participant or 
 * a visitor).
 * 
 * A moderator is the most powerful occupant within the context of the room, and can to some extent manage 
 * other occupants' roles in the room. A participant has fewer privileges than a moderator, although he or 
 * she always has the right to speak. A visitor is a more restricted role within the context of a moderated 
 * room, since visitors are not allowed to send messages to all occupants.
 * 
 * Roles are granted, revoked, and maintained based on the occupant's room nickname or full JID rather than 
 * bare JID. The privileges associated with these roles, as well as the actions that trigger changes in roles, 
 * are defined below.
 * 
 * Information about roles MUST be sent in all presence stanzas generated or reflected by the room and thus 
 * sent to occupants.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0045", section = "5.1", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public enum Role {

    Moderator, None, Participant, Visitor;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    @SpecCompliant(spec = "xep-0045", section = "7.1.4", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
    public static Role getRole(Affiliation affiliation, EnumSet<RoomType> roomTypes) {
        switch (affiliation) {
        case Owner:
        case Admin:
            return Moderator;
        case Member:
            return Participant;
        case None:
            if (roomTypes.contains(RoomType.MembersOnly)) {
                return None;
            } else if (roomTypes.contains(RoomType.Moderated)) {
                return Visitor;
            } else {
                return Participant;
            }
        default:
            // no role for Outcast
            return null;
        }
    }

    public static Role fromString(String value) {
        if (Moderator.toString().equals(value))
            return Moderator;
        else if (None.toString().equals(value))
            return None;
        else if (Participant.toString().equals(value))
            return Participant;
        else if (Visitor.toString().equals(value))
            return Visitor;
        else
            throw new IllegalArgumentException("Unknown role: " + value);
    }
}
