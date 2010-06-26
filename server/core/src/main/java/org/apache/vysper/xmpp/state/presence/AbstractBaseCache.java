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
 * Base Cache implementation class. Has utility functions and implements
 * common validations here. Other classes can extend this class and implement
 * only the put, get and remove functions.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractBaseCache implements LatestPresenceCache {

    /**
     * Validates basic Entity Data. Current implementation has following checks
     * 1. Entity cannot be null
     * 2. Resource cannot be null 
     *
     * @param entity    Entity to be verified
     */
    protected void checkEntry(Entity entity) {
        if (entity == null) {
            throw new PresenceCachingException("Entity cannot be null");
        }

        if (entity.getResource() == null) {
            throw new PresenceCachingException("presense stanzas are cached per resource, failure for "
                    + entity.getFullQualifiedName());
        }
    }

    /**
     * @inheritDoc
     */
    public void put(Entity entity, PresenceStanza presenceStanza) throws PresenceCachingException {
        checkEntry(entity);

        // Can't store null entity
        if (presenceStanza == null) {
            throw new PresenceCachingException("Presence Stanza cannot be null");
        }

        put0(entity, presenceStanza);
    }

    /**
     * Put the entry in the cache. The basic validations have been done in the
     * calling function.
     *
     * @param entity            Key for the PresenceStanza
     * @param presenceStanza    PresenceStanza to be stored
     * @throws PresenceCachingException
     */
    protected abstract void put0(Entity entity, PresenceStanza presenceStanza) throws PresenceCachingException;

    public PresenceStanza get(Entity entity) throws PresenceCachingException {
        checkEntry(entity);
        return get0(entity);
    }

    /**
     * returns the PresenceStanza, with Entity as key
     *
     * @param entity        Key for the Cache entry
     * @return              PresenceStanza related with the key
     * @throws PresenceCachingException
     */
    protected abstract PresenceStanza get0(Entity entity) throws PresenceCachingException;
}