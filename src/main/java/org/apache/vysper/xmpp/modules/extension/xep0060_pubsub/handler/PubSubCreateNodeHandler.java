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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;


/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class PubSubCreateNodeHandler extends AbstractPubSubGeneralHandler {

	/**
	 * @param root
	 */
	public PubSubCreateNodeHandler(CollectionNode root) {
		super(root);
	}

	@Override
	protected String getWorkerElement() {
		return "create";
	}

}
