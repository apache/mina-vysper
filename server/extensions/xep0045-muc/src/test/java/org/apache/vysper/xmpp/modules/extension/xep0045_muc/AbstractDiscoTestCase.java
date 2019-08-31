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

import static org.mockito.Mockito.mock;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import junit.framework.TestCase;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractDiscoTestCase extends TestCase {

    protected static final String SUBDOMAIN = "chat";

    protected static final String SERVERDOMAIN = "vysper.org";

    protected static final String MODULEDOMAIN = SUBDOMAIN + "." + SERVERDOMAIN;

    protected static final Entity SERVER_JID = EntityImpl.parseUnchecked(SERVERDOMAIN);

    protected static final Entity MODULE_JID = EntityImpl.parseUnchecked(MODULEDOMAIN);

    protected static final Entity USER_JID = EntityImpl.parseUnchecked("user@" + SERVERDOMAIN);

    protected DefaultServerRuntimeContext serverRuntimeContext;

    private ServiceCollector serviceCollector;

    protected abstract Module getModule();

    protected abstract void assertResponse(XMLElement queryElement) throws Exception;

    protected abstract StanzaBuilder buildRequest();

    protected abstract IQHandler createDiscoIQHandler();

    protected abstract Entity getTo();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        serviceCollector = new ServiceCollector();
        serverRuntimeContext = new DefaultServerRuntimeContext(SERVER_JID, mock(StanzaRelay.class));
        serverRuntimeContext.registerServerRuntimeContextService(serviceCollector);

    }

    public void testDisco() throws Exception {
        serverRuntimeContext.registerComponent((Component) getModule());

        IQHandler infoIQHandler = createDiscoIQHandler();

        StanzaBuilder request = buildRequest();

        RecordingStanzaBroker stanzaBroker = new RecordingStanzaBroker();
        infoIQHandler.execute(request.build(), serverRuntimeContext, false, new TestSessionContext(serverRuntimeContext,
                new SessionStateHolder(), serverRuntimeContext.getStanzaRelay()), null, stanzaBroker);
        Stanza resultStanza = stanzaBroker.getUniqueStanzaWrittenToSession();

        assertEquals("Disco request must not return error", "result", resultStanza.getAttributeValue("type"));
        XMLElement queryElement = resultStanza.getFirstInnerElement();

        assertResponse(queryElement);
    }
}
