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
package org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * provides infos about the available command for a specific module
 */
public class CommandInfo {

    protected Entity jid;
    protected String node;
    protected String name;

    public CommandInfo(Entity jid, String node, String name) {
        this.jid = jid;
        this.node = node;
        this.name = name;
    }

    public CommandInfo(String node, String name) {
        this.node = node;
        this.name = name;
    }

    public Entity getJid() {
        return jid;
    }

    public String getNode() {
        return node;
    }

    public String getName() {
        return name;
    }
}
