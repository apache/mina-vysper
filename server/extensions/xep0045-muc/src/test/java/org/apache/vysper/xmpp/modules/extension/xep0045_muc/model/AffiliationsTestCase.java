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

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class AffiliationsTestCase extends TestCase {

    private static final Entity JID1 = EntityImpl.parseUnchecked("user1@vysper.org/res");

    private static final Entity JID2 = EntityImpl.parseUnchecked("user1@vysper.org/other");

    private static final Entity JID3 = EntityImpl.parseUnchecked("user2@vysper.org/res");

    public void testAddAndGet() {
        Affiliations affiliations = new Affiliations();
        affiliations.add(JID1, Affiliation.Admin);

        assertEquals(Affiliation.Admin, affiliations.getAffiliation(JID1));
        // get with different resource
        assertEquals(Affiliation.Admin, affiliations.getAffiliation(JID2));

        assertEquals(null, affiliations.getAffiliation(JID3));
    }

    public void testUpdate() {
        Affiliations affiliations = new Affiliations();
        affiliations.add(JID1, Affiliation.Admin);

        assertEquals(Affiliation.Admin, affiliations.getAffiliation(JID1));
        // add with different resource and affiliation
        affiliations.add(JID2, Affiliation.Member);

        assertEquals(Affiliation.Member, affiliations.getAffiliation(JID1));
    }

    public void testRemove() {
        Affiliations affiliations = new Affiliations();
        affiliations.add(JID1, Affiliation.Admin);

        assertEquals(Affiliation.Admin, affiliations.getAffiliation(JID1));

        affiliations.remove(JID2);

        assertEquals(null, affiliations.getAffiliation(JID1));
    }
}
