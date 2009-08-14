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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import junit.framework.TestCase;

import org.apache.vysper.TestUtil;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractDiscoTestCase extends TestCase {
    
    protected static final Entity SERVER_JID = TestUtil.parseUnchecked("vysper.org");
    protected static final Entity MODULE_JID = TestUtil.parseUnchecked("chat.vysper.org");
    protected static final Entity USER_JID = TestUtil.parseUnchecked("user@vysper.org");

    protected abstract Module getModule();

    protected abstract void assertResponse(XMLElement queryElement)
            throws Exception;

    protected abstract StanzaBuilder buildRequest();

    protected abstract void addListener(ServiceCollector serviceCollector);
    
    protected abstract IQHandler createDiscoIQHandler();
    
    public void testDisco() throws Exception {
        ServiceCollector serviceCollector = new ServiceCollector();
        addListener(serviceCollector);

        DefaultServerRuntimeContext runtimeContext = new DefaultServerRuntimeContext(SERVER_JID, null);
        runtimeContext.registerServerRuntimeContextService(serviceCollector);

        IQHandler infoIQHandler = createDiscoIQHandler();

        StanzaBuilder request = buildRequest();
        
        ResponseStanzaContainer resultStanzaContainer = infoIQHandler.execute(request.getFinalStanza(), runtimeContext, false, new TestSessionContext(runtimeContext, new SessionStateHolder()), null);
        Stanza resultStanza = resultStanzaContainer.getResponseStanza();

        XMLElement queryElement = resultStanza.getFirstInnerElement();
        
        assertResponse(queryElement);
    }
}
