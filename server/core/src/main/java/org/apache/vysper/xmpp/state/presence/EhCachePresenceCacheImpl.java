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

import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.PresenceStanza;

/**
 * EhCache based cache Presence cache implementation. The imlpementation stores
 * the PresenceStanza into two cache's. One is for Entity as a key. However to
 * cater to request for bare JID's, we store the smae entry into another cahce
 * with JID as a key. Since the PresenceStanza is stores as a reference, the
 * memory penalty is not too much.
 *
 * The Policy for the cahce is LRU. The cahce is created from ehcache.xml file
 * The cahce is in-memory only and doesn't store data to the same. If disk
 * storage is needed, the same can be achieved by updating the ehcache.xml file.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class EhCachePresenceCacheImpl extends AbstractBaseCache {

    // Key to fetch Presence Cache
    private static final String PRESENCE_CACHE = "PresenceCache";

    // Key to fetch JID Presence Cache
    private static final String JID_PRESENCE_CACHE = "JIDCache";

    // Cache instance to store the presence information against Entity
    private Cache presenceCache = null;

    // Cache instance to store presence information against JID
    private Cache jidPresenceCache = null;

    // Default ehcache configuration file
    private static final String DEFAULT_EHCACHE_CONFIG_FILE = "/ehcache.xml";

    /**
     * Defaulr Contructor
     *
     * @param configFile    ehcache configuration file
     */
    public EhCachePresenceCacheImpl(String configFile) {
        createCache(configFile);
    }

    /**
     * Create the ehcache based on ehcache.xml file
     * Create two cache instances to store PresenceStanza against Entity and
     * JID
     *
     * @param configFile    ehcache configuration file
     */
    protected void createCache(String configFile) {
        if (configFile == null) {
            configFile = DEFAULT_EHCACHE_CONFIG_FILE;
        }
        URL configFileURL = getClass().getResource(configFile);
        if (configFileURL == null) {
            throw new RuntimeException(String.format("Ehcache configuration file %s not found on classpath", configFile));
        }
		
		// use the provided config file
        CacheManager cacheManager = CacheManager.create(configFileURL);

        presenceCache = cacheManager.getCache(PRESENCE_CACHE);
        jidPresenceCache = cacheManager.getCache(JID_PRESENCE_CACHE);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void put0(Entity entity, PresenceStanza presenceStanza) throws PresenceCachingException {
        // Create EhCache elements to be stored
        Element cacheElement = new Element(entity, presenceStanza);
        Element jidElement = new Element(entity.getBareJID(), presenceStanza);

        presenceCache.put(cacheElement);
        jidPresenceCache.put(jidElement);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected PresenceStanza get0(Entity entity) throws PresenceCachingException {
        // Get the Element from cache
        Element cacheElement = presenceCache.get(entity);

        // return the value from presence cache
        if (cacheElement != null) {
            return (PresenceStanza) cacheElement.getObjectValue();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public PresenceStanza getForBareJID(Entity entity) throws PresenceCachingException {
        // return null for null entries
        if (entity == null) {
            return null;
        }

        Element cacheElement = jidPresenceCache.get(entity);

        // return the value from presence cache
        if (cacheElement != null) {
            return (PresenceStanza) cacheElement.getObjectValue();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public void remove(Entity entity) {
        // if entity is null, keep silent
        if (entity == null) {
            return;
        }

        // Remove the cache from presence and jid presence cache
        presenceCache.remove(entity);
        jidPresenceCache.remove(entity.getBareJID());
    }
}