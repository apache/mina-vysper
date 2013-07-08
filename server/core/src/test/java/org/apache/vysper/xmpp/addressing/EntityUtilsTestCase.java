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
package org.apache.vysper.xmpp.addressing;

import junit.framework.TestCase;

public class EntityUtilsTestCase extends TestCase {

    private Entity server = EntityImpl.parseUnchecked("vysper.org");
    
    public void testIsComponent() {
        assertAddressingServerComponent("foo.vysper.org");
        assertAddressingServerComponent("bar@foo.vysper.org");
        assertAddressingServerComponent("bar@foo.vysper.org/xyz");
        assertAddressingServerComponent("bar.foo.vysper.org");
        assertNotAddressingServerComponent("vysper.org");
        assertNotAddressingServerComponent("foovysper.org");
        assertNotAddressingServerComponent("foo.org");
    }

    public void testIsInternal() {
        assertAddressingServer("vysper.org");
        assertNotAddressingServer("foo.vysper.org");
        assertAddressingServer("bar@vysper.org");
        assertAddressingServer("bar@vysper.org/xyz");
        assertNotAddressingServer("foovysper.org");
        assertNotAddressingServer("foo.org");
    
    }
    
    public void testIsMixedCasing() {
        assertAddressingServer("vYsper.org");
        assertAddressingServer("VYSPER.ORG");
        assertAddressingServer("vysper.org");
        assertAddressingServer("vysper.ORG");
        assertAddressingServer("Vysper.ORG");
        assertAddressingServer("Vysper.Org");
        assertNotAddressingServer("foo.vYsper.org");
        assertAddressingServer("bar@vYsper.oRg");
        assertAddressingServer("bar@vYsper.ORG/xyz");
        assertNotAddressingServer("foovYsper.org");
        assertNotAddressingServer("foo.org");
        assertNotAddressingServer("fOO.org");
    }

    
    private void assertAddressingServerComponent(String entity) {
        assertTrue(EntityUtils.isAddressingServerComponent(EntityImpl.parseUnchecked(entity), server));
    }

    private void assertNotAddressingServerComponent(String entity) {
        assertFalse(EntityUtils.isAddressingServerComponent(EntityImpl.parseUnchecked(entity), server));
    }

    private void assertAddressingServer(String entity) {
        assertTrue(EntityUtils.isAddressingServer(EntityImpl.parseUnchecked(entity), server));
    }

    private void assertNotAddressingServer(String entity) {
        assertFalse(EntityUtils.isAddressingServer(EntityImpl.parseUnchecked(entity), server));
    }

}
