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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * Describes the persistent affiliations for a room
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Affiliations {

    // TODO should be loaded from the storage provider
    // keyed by bare JIDs
    private Map<Entity, Affiliation> affiliations = new HashMap<Entity, Affiliation>();

    public void add(Entity user, Affiliation affiliation) {
        affiliations.put(user.getBareJID(), affiliation);
    }

    public void remove(Entity user) {
        affiliations.remove(user.getBareJID());
    }

    public Affiliation getAffiliation(Entity user) {
        return affiliations.get(user.getBareJID());
    }

    public Collection<Entity> getByAffiliation(Affiliation affiliation) {
        List<Entity> matching = new ArrayList<Entity>();
        for(Entry<Entity, Affiliation> entry : affiliations.entrySet()) {
            if(entry.getValue() == affiliation) {
                matching.add(entry.getKey());
            }
        }
        return matching;
    }
}
