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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Status extends XMLElement {

    public enum StatusCode {
        /** Inform user that any occupant is allowed to see the user's full JID */
        ROOM_NON_ANONYMOUS(100),
        /** Inform user that his or her affiliation changed while not in the room */
        AFFILIATION_CHANGE(101),
        /** Inform occupants that room now shows unavailable members */
        SHOWING_UNAVAILABLE_MEMBERS(102),
        /** Inform occupants that room now does not show unavailable members */
        HIDING_UNAVAILABLE_MEMBERS(103),
        /** Inform occupants that a non-privacy-related room configuration change has occurred */
        NON_PRIVACY_CHANGE(104),
        /** Inform user that presence refers to one of its own room occupants */
        OWN_PRESENCE(110),
        /** Inform occupants that room logging is now enabled */
        ROOM_LOGGING_ENABLED(170),
        /** Inform occupants that room logging is now disabled */
        ROOM_LOGGING_DISABLED(171),
        /** Inform occupants that the room is now non-anonymous */
        ROOM_NOW_NON_ANONYMOUS(172),
        /** Inform occupants that the room is now semi-anonymous */
        ROOM_NOW_SEMI_ANONYMOUS(173),
        /** Inform occupants that the room is now fully-anonymous */
        ROOM_NOW_FULLY_ANONYMOUS(174),
        /** Inform user that a new room has been created */
        ROOM_CREATED(201),
        /** Inform user that the service has assigned or modified the occupant's roomnick */
        NICK_MODIFIED(210),
        /** Inform user that he or she has been banned from the room */
        BEEN_BANNED(301),
        /** Inform all occupants of new room nickname */
        NEW_NICK(303),
        /** Inform user that he or she has been kicked from the room */
        BEEN_KICKED(307),
        /** Inform user that he or she is being removed from the room because of an affiliation change */
        REMOVED_BY_AFFILIATION(321),
        /** Inform user that he or she is being removed from the room because the room has been changed 
         * to members-only and the user is not a member */
        REMOVED_BY_MEMBERSHIP(322),
        /** Inform user that he or she is being removed from the room because of a system shutdown */
        REMOVED_BY_SHUTDOWN(323);

        private int statusCode;

        private StatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public int code() {
            return statusCode;
        }
    }

    public Status(StatusCode code) {
        this(code, null);
    }

    public Status(StatusCode code, String message) {
        super(NamespaceURIs.XEP0045_MUC, "status", null, createAttributes(code), createFragments(message));
    }

    private static List<Attribute> createAttributes(StatusCode code) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        if (code != null)
            attributes.add(new Attribute("code", Integer.toString(code.code())));
        return attributes;
    }

    private static List<XMLFragment> createFragments(String message) {
        List<XMLFragment> fragments = new ArrayList<XMLFragment>();
        if (message != null)
            fragments.add(new XMLText(message));
        return fragments;
    }

    public Status(String message) {
        this(null, message);
    }
}
