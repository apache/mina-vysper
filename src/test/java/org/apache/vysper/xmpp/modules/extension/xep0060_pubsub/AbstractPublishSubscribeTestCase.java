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

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceState;


/**
 * The abstract base class for all pubsub related tests.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public abstract class AbstractPublishSubscribeTestCase extends TestCase {
	private TestSessionContext sessionContext  = null;
    protected Entity clientBare = null;
    protected Entity client = null;
    protected Entity pubsub = null;
    protected IQHandler handler = null;
    protected CollectionNode root = null;
    
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
		clientBare = new EntityImpl("tester", "vysper.org", null);
        sessionContext.setInitiatingEntity(clientBare);

        String boundResourceId = sessionContext.bindResource();
        client = new EntityImpl(clientBare, boundResourceId);
		pubsub = EntityImpl.parse("pubsub.vysper.org/news");
		root = new CollectionNode();

        setResourceConnected(sessionContext, boundResourceId);
        
        handler = getHandler();
	}
	
	private void setResourceConnected(SessionContext sessionContext, String boundResourceId) {
		sessionContext.getServerRuntimeContext().getResourceRegistry().setResourceState(boundResourceId, ResourceState.CONNECTED);
	}

	/**
	 * Override and provide the Handler to be tested. A new
	 * handler will be created for each test.
	 * 
	 * @return the instantiated handler to be tested
	 */
	protected abstract IQHandler getHandler();
	
	/**
	 * Override and provide the Namespace the pubsub element lies within.
	 * 
	 * @return the namespace for the IQ stanza as String
	 */
	protected abstract String getNamespace();
	
	/**
	 * Override and provide a optional inner element (within the IQ/pubsub elements).
	 * 
	 * @param sb the StanzaBuilder currently used
	 * @return the (modified) StanzaBuilder
	 */
	protected abstract StanzaBuilder buildInnerElement(StanzaBuilder sb);
	
	/**
	 * Override and define the IQ stanza's type (get or set)
	 * 
	 * @return Type of Stanza @see {@link IQStanzaType}
	 */
	protected abstract IQStanzaType getStanzaType();
	
	protected Stanza getStanza() {
		StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(client, pubsub, getStanzaType(), "id1");
        stanzaBuilder.startInnerElement("pubsub");
        stanzaBuilder.addNamespaceAttribute(getNamespace());
        
        buildInnerElement(stanzaBuilder);
        
        stanzaBuilder.endInnerElement();
        
        return stanzaBuilder.getFinalStanza();
	}
	
	protected ResponseStanzaContainer sendStanza(Stanza toSend, boolean isOutboundStanza) {
		return handler.execute(toSend, sessionContext.getServerRuntimeContext(), isOutboundStanza, sessionContext, null);
	}
	
	public void testSimpleStanza() {
		Stanza stanza = getStanza();
        
        assertTrue(handler.verify(stanza));
	}
}
