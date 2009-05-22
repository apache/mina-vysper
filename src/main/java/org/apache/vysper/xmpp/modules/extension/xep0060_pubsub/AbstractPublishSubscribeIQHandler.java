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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.uuid.JVMBuiltinUUIDGenerator;
import org.apache.vysper.xmpp.uuid.UUIDGenerator;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * Handles PubSub stanzas.
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public abstract class AbstractPublishSubscribeIQHandler extends DefaultIQHandler {
	
	protected CollectionNode root;
	protected UUIDGenerator idGenerator;
	
	public AbstractPublishSubscribeIQHandler(CollectionNode root) {
		this.root = root;
		this.idGenerator = new JVMBuiltinUUIDGenerator();
	}
	
	@Override
	protected boolean verifyNamespace(Stanza stanza) {
		return verifyInnerNamespace(stanza, getNamespace());
	}
	
	@Override
	protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "pubsub") 
        	&& verifySingleElementParameter(stanza.getFirstInnerElement(), getWorkerElement());
	}
	
	protected boolean verifySingleElementParameter(XMLElement pubsub, String element) {
		return pubsub.getVerifier().subElementsPresentExact(1)
			&& pubsub.getVerifier().subElementPresent(element);		
	}

	protected abstract String getNamespace();
	protected abstract String getWorkerElement();
}
