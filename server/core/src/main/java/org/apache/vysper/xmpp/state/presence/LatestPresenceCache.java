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
package org.apache.vysper.xmpp.state.presence;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.PresenceStanza;

/**
 * Keeps the latest presence for a resource
 */
public interface LatestPresenceCache {

    /**
     * puts the latest presence stanza for the resource into the cache
     * @param entity the entity with the resource id set
     * @param presenceStanza latest presence stanza
     * @throws PresenceCachingException
     */
    void put(Entity entity, PresenceStanza presenceStanza) throws PresenceCachingException;

    /**
     * retrieves the latest presence stanza for the resource from the cache
     * @param entity the entity with the resource id set
     * @return
     * @throws PresenceCachingException
     */
    PresenceStanza get(Entity entity) throws PresenceCachingException;

    /**
     * retrieves the latest presence stanza for the resource from the cache
     * @param entity the entity with the resource id set
     * @return
     * @throws PresenceCachingException
     */
    PresenceStanza getForBareJID(Entity entity) throws PresenceCachingException;

    /**
     * removes the stanza for this particular resource (on UNAVAILABLE or session termination) 
     * @param entity the entity with the resource id set
     */
    void remove(Entity entity);
}
