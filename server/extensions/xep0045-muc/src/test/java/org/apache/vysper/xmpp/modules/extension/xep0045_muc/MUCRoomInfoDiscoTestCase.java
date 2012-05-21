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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MUCRoomInfoDiscoTestCase extends AbstractInfoDiscoTestCase {

    private static final Entity ROOM_JID = EntityImpl.parseUnchecked("jid1@" + MODULEDOMAIN);

    private MUCModule module;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Conference conference = new Conference("Foo", new MUCFeatures());
        conference.createRoom(ROOM_JID, "Room1", RoomType.Hidden, RoomType.PasswordProtected);
        module = new MUCModule(SUBDOMAIN, conference);
        module.initialize(serverRuntimeContext);
    }

    @Override
    protected Module getModule() {
        return module;
    }

    @Override
    protected Identity getExpectedIdentity() {
        return new Identity("conference", "text", "Room1");
    }

    @Override
    protected List<String> getExpectedFeatures() {
        return Arrays.asList(NamespaceURIs.XEP0045_MUC, "muc_passwordprotected", "muc_hidden");
    }

    @Override
    protected Entity getTo() {
        return ROOM_JID;
    }
}
