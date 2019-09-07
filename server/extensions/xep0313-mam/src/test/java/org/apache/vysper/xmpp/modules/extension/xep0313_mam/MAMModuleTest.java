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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.junit.Test;

import junit.framework.Assert;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MAMModuleTest {

    private MAMModule tested = new MAMModule();

    @Test
    public void nameMustBeProvided() {
        Assert.assertNotNull(tested.getName());
    }

    @Test
    public void versionMustBeProvided() {
        Assert.assertNotNull(tested.getVersion());
    }

    @Test
    public void getServerInfosFor() throws ServiceDiscoveryRequestException {
        List<ServerInfoRequestListener> serverInfoRequestListeners = new ArrayList<>();

        tested.addServerInfoRequestListeners(serverInfoRequestListeners);

        Assert.assertEquals(1, serverInfoRequestListeners.size());

        List<InfoElement> infoElements = serverInfoRequestListeners.get(0)
                .getServerInfosFor(new InfoRequest(null, null, null, null));

        Assert.assertEquals(2, infoElements.size());
        Assert.assertTrue(infoElements.get(0) instanceof Feature);
        Assert.assertTrue(infoElements.get(1) instanceof Feature);
        Assert.assertEquals(NamespaceURIs.XEP0359_STANZA_IDS, ((Feature) infoElements.get(0)).getVar());
        Assert.assertEquals(NamespaceURIs.JABBER_X_DATA, ((Feature) infoElements.get(1)).getVar());
    }

}