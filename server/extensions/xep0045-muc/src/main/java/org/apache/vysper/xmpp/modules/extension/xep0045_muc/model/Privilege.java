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
 * For the most part, roles exist in a hierarchy. 
 * For instance, a participant can do anything a visitor can do, and a moderator 
 * can do anything a participant can do. Each role has privileges not possessed 
 * by the next-lowest role; these privileges are specified in the following table 
 * as defaults (an implementation MAY provide configuration options that override 
 * these defaults). 
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0045", section = "5.1.1", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public enum Privilege {

    PresentInRoom, ReceiveMessages, ReceiveOccupantPresence, PresenceBroadcastedToRoom, ChangeAvailabilityStatus, ChangeRoomNickname, SendPrivateMessages, InviteOtherUsers, SendMessagesToAll, ModifySubject, KickParticipantsAndVisitors, GrantVoice, RevokeVoice,

    EnterOpenRoom, RegisterWithOpenRoom, RetrieveMemberList, EnterMembersOnlyRoom, BanMembersAndUnaffiliatedUsers, EditMemberList, EditModeratorList, EditAdminList, EditOwnerList, ChangeRoomDefinition, DestroyRoom;

    /**
     * Privileges for a moderator
     */
    private static EnumSet<Privilege> MODERATOR_PRIVILEGES = EnumSet.of(PresentInRoom, ReceiveMessages,
            ReceiveOccupantPresence, PresenceBroadcastedToRoom, ChangeAvailabilityStatus, ChangeRoomNickname,
            SendPrivateMessages, InviteOtherUsers, SendMessagesToAll, ModifySubject, KickParticipantsAndVisitors,
            GrantVoice, RevokeVoice);

    /**
     * Privileges for a participant
     */
    private static EnumSet<Privilege> PARTICIPANT_PRIVILEGES = EnumSet.of(PresentInRoom, ReceiveMessages,
            ReceiveOccupantPresence, PresenceBroadcastedToRoom, ChangeAvailabilityStatus, ChangeRoomNickname,
            SendPrivateMessages, InviteOtherUsers, SendMessagesToAll, ModifySubject);

    /**
     * Privileges for a visitor
     */
    private static EnumSet<Privilege> VISITOR_PRIVILEGES = EnumSet.of(PresentInRoom, ReceiveMessages,
            ReceiveOccupantPresence, PresenceBroadcastedToRoom, ChangeAvailabilityStatus, ChangeRoomNickname,
            SendPrivateMessages, InviteOtherUsers);

    /**
     * Privileges for a none
     */
    private static EnumSet<Privilege> NONE_ROLE_PRIVILEGES = EnumSet.noneOf(Privilege.class);

    /**
     * Privileges for an {@link Affiliation} owner
     */
    private static EnumSet<Privilege> OWNER_PRIVILEGES = EnumSet.of(EnterOpenRoom, RegisterWithOpenRoom,
            RetrieveMemberList, EnterMembersOnlyRoom, BanMembersAndUnaffiliatedUsers, EditMemberList,
            EditModeratorList, EditAdminList, EditOwnerList, ChangeRoomDefinition, DestroyRoom);

    /**
     * Privileges for an {@link Affiliation} admin
     */
    private static EnumSet<Privilege> ADMIN_PRIVILEGES = EnumSet
            .of(EnterOpenRoom, RegisterWithOpenRoom, RetrieveMemberList, EnterMembersOnlyRoom,
                    BanMembersAndUnaffiliatedUsers, EditMemberList, EditModeratorList);

    /**
     * Privileges for an {@link Affiliation} member
     */
    private static EnumSet<Privilege> MEMBER_PRIVILEGES = EnumSet.of(EnterOpenRoom, RegisterWithOpenRoom,
            RetrieveMemberList, EnterMembersOnlyRoom);

    /**
     * Privileges for an {@link Affiliation} none
     */
    private static EnumSet<Privilege> NONE_AFFILIATION_PRIVILEGES = EnumSet.of(EnterOpenRoom, RegisterWithOpenRoom);

    /**
     * Privileges for an {@link Affiliation} outcast
     */
    private static EnumSet<Privilege> OUTCATS_PRIVILEGES = EnumSet.noneOf(Privilege.class);

    /**
     * Get the privileges for the specified {@link Role}
     * @param role The {@link Role} to look up privileges for
     * @return The privileges for the specified role
     */
    public static EnumSet<Privilege> getPrivileges(Role role) {
        switch (role) {
        case Moderator:
            return MODERATOR_PRIVILEGES;
        case Participant:
            return PARTICIPANT_PRIVILEGES;
        case Visitor:
            return VISITOR_PRIVILEGES;
        default:
            return NONE_ROLE_PRIVILEGES;
        }
    }

    /**
     * Get the privileges for the specified {@link Affiliation}
     * @param role The {@link Affiliation} to look up privileges for
     * @return The privileges for the specified affiliation
     */
    public static EnumSet<Privilege> getPrivileges(Affiliation affiliation) {
        switch (affiliation) {
        case Owner:
            return OWNER_PRIVILEGES;
        case Admin:
            return ADMIN_PRIVILEGES;
        case Member:
            return MEMBER_PRIVILEGES;
        case None:
            return NONE_AFFILIATION_PRIVILEGES;
        default:
            return OUTCATS_PRIVILEGES;
        }
    }
}
