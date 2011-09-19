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
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.TestUser;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class EhCachePresenceCacheImplTest extends LatestPresenceCacheTestCase {

    LatestPresenceCache presenceCache = new EhCachePresenceCacheImpl(null);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected LatestPresenceCache getCache() {
        return presenceCache;
    }
	
	/**
	 * Test the use of custom file other than ehcache.xml for creation of Ehcache
	 */
	public void testCreateCacheCustomFile() {
		try {
			LatestPresenceCache presenceTestCache = new EhCachePresenceCacheImpl("/ehcache-test.xml");
			assertNotNull(presenceTestCache);
		} catch (Exception exception) {
			exception.printStackTrace();
			fail("Cache creation should have successful");
		}	    
	}

    /**
     * Test Cache is created properly
     */
    public void testCreateCache() {
        assertNotNull(presenceCache);
    }

    public void testPut() {
        presenceCache.put(getEntity(), getPresenceStanza(initiatingUser));
        assertNotNull(presenceCache.get(getEntity()));
    }

    public void testRemove() {
        Entity entity = getEntity();
        presenceCache.put(entity, getPresenceStanza(initiatingUser));
        presenceCache.remove(entity);

        // It should return null
        assertNull("Entry should be null", presenceCache.get(entity));
        assertNull("Entry should be null in JID Cache", presenceCache.getForBareJID(entity));
    }

    public void testGetNullParam() {
        try {
            presenceCache.get(null);
        } catch (PresenceCachingException ex) {
            assertTrue("Exception was expected was null entry", true);
        } catch (Exception e) {
            fail("Only PresenceCachingException is expected");
        }
    }

    public void testPutNullEntityParam() {
        try {
            presenceCache.put(null, getPresenceStanza(initiatingUser));
        } catch (PresenceCachingException ex) {
            assertTrue("Exception was expected was null entry", true);
        } catch (Exception e) {
            fail("Only PresenceCachingException is expected");
        }
    }

    public void testPutNullPresenceParam() {
        try {
            presenceCache.put(getEntity(), null);
        } catch (PresenceCachingException ex) {
            assertTrue("Exception was expected was null entry", true);
        } catch (Exception e) {
            fail("Only PresenceCachingException is expected");
        }
    }

    /**
     * Returns an Entity to be used as a key
     * @return  Entity instance
     */
    protected Entity getEntity() {
        try {
            return EntityImpl.parse("tester@apache.org/test");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a PresenceStanza instance to be used as value for cache
     * 
     * @param user  User whose Presence Information is to be created
     * @return  Presence Information of the User
     */
    protected PresenceStanza getPresenceStanza(TestUser user) {
        XMPPCoreStanza initialPresence = XMPPCoreStanza.getWrapper(StanzaBuilder.createPresenceStanza(
                user.getEntityFQ(), null, null, null, null, null).build());
        return (PresenceStanza) initialPresence;
    }

}