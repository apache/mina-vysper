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
package org.apache.vysper.xmpp.modules.servicediscovery.management;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class InfoRequest {

    private Entity from;

    private Entity to;

    private String node = null;

    private String id;

    public InfoRequest(Entity from, Entity to, String node, String id) {
        this.from = from;
        this.to = to;
        this.node = node;
        this.id = id;
    }

    public Entity getFrom() {
        return from;
    }

    public Entity getTo() {
        return to;
    }

    public String getNode() {
        return node;
    }

    /**
     * The ID of the original request
     */
    public String getID() {
        return id;
    }
}
