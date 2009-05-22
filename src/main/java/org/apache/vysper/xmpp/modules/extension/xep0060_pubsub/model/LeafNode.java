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

import org.apache.vysper.xmpp.addressing.Entity;

import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class LeafNode {
	
	protected String name;
	protected Map<String,Entity> setSubscriber;
	
	/**
	 * Creates a new LeafNode with the specified name.
	 * @param name the name of the node 
	 */
	public LeafNode(String name) {
		this.name = name;
		this.setSubscriber = new TreeMap<String, Entity>();
	}
	
	public void subscribe(String id, Entity subscriber) {
		setSubscriber.put(id,subscriber);
	}
	
	public boolean isSubscribed(Entity subscriber) {
		return setSubscriber.containsValue(subscriber);
	}
	
	public boolean isSubscribed(String subscriptionId) {
		return setSubscriber.containsKey(subscriptionId);
	}
}
