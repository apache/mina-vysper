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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.inttest;

import org.jivesoftware.smackx.muc.ParticipantStatusListener;

/**
 */
public class ParticipantStatusListenerAdapter implements ParticipantStatusListener {

    public void adminGranted(String participant) {
    }

    public void adminRevoked(String participant) {
    }

    public void banned(String participant, String actor, String reason) {
    }

    public void joined(String participant) {
    }

    public void kicked(String participant, String actor, String reason) {
    }

    public void left(String participant) {
    }

    public void membershipGranted(String participant) {
    }

    public void membershipRevoked(String participant) {
    }

    public void moderatorGranted(String participant) {
    }

    public void moderatorRevoked(String participant) {
    }

    public void nicknameChanged(String participant, String newNickname) {
    }

    public void ownershipGranted(String participant) {
    }

    public void ownershipRevoked(String participant) {
    }

    public void voiceGranted(String participant) {
    }

    public void voiceRevoked(String participant) {
    }
}
