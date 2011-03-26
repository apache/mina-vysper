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
package org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback;
import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;


public class DialbackIdGeneratorTestCase extends TestCase {

    private Entity receiving = EntityImpl.parseUnchecked("xmpp.example.com");
    private Entity originating = EntityImpl.parseUnchecked("example.org");
    private String streamId = "D60000229F";
    
    public void testId() {
        DialbackIdGenerator generator = new DialbackIdGenerator();
        String id = generator.generate(receiving, originating, streamId);
        
        Assert.assertTrue(generator.verify(id, receiving, originating, streamId));
        
    }

    public void testNotValidId() {
        DialbackIdGenerator generator = new DialbackIdGenerator();
        Assert.assertFalse(generator.verify("1234567890", receiving, originating, streamId));
    }
}
