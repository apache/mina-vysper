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
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

/**
 */
public class ParticipantStatusListenerAdapter implements ParticipantStatusListener {

    @Override
    public void joined(EntityFullJid participant) {
        
    }

    @Override
    public void left(EntityFullJid participant) {

    }

    @Override
    public void kicked(EntityFullJid participant, Jid actor, String reason) {

    }

    @Override
    public void voiceGranted(EntityFullJid participant) {

    }

    @Override
    public void voiceRevoked(EntityFullJid participant) {

    }

    @Override
    public void banned(EntityFullJid participant, Jid actor, String reason) {

    }

    @Override
    public void membershipGranted(EntityFullJid participant) {

    }

    @Override
    public void membershipRevoked(EntityFullJid participant) {

    }

    @Override
    public void moderatorGranted(EntityFullJid participant) {

    }

    @Override
    public void moderatorRevoked(EntityFullJid participant) {

    }

    @Override
    public void ownershipGranted(EntityFullJid participant) {

    }

    @Override
    public void ownershipRevoked(EntityFullJid participant) {

    }

    @Override
    public void adminGranted(EntityFullJid participant) {

    }

    @Override
    public void adminRevoked(EntityFullJid participant) {

    }

    @Override
    public void nicknameChanged(EntityFullJid participant, Resourcepart newNickname) {

    }
}
