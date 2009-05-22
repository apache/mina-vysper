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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model;

import junit.framework.TestCase;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class CollectionNodeTestCase extends TestCase  {
	
	protected CollectionNode collection;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		collection = new CollectionNode();
	}
	
	public void testCreateNode() throws Exception {
		LeafNode test1 = collection.createNode("test1");
		assertNotNull(test1);
	}

	public void testCreateNodeTwice() {
		try {
			collection.createNode("test1");
			collection.createNode("test1");
			fail();
		} catch(DuplicateNodeException e) {
			// ok
		}
	}
	
	public void testInsertFind() throws Exception {
		LeafNode insertedNode = collection.createNode("test1");
		LeafNode foundNode = collection.find("test1");
		assertEquals(insertedNode, foundNode);
	}
	
	public void testFindNone() {
		assertNull(collection.find("doesnotexist"));
	}
	
}
