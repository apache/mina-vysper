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
package org.apache.vysper.xmpp.modules.extension.xep0049_privatedata;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.junit.Test;

/**
 */
public class PrivateDataModuleTestCase {

    private PrivateDataModule module = new PrivateDataModule();
    
    @Test
    public void nameMustBeProvided() {
        Assert.assertNotNull(module.getName());
    }

    @Test
    public void versionMustBeProvided() {
        Assert.assertNotNull(module.getVersion());
    }
    
    @Test
    public void getServerInfosFor() throws ServiceDiscoveryRequestException {
        List<ServerInfoRequestListener> serverInfoRequestListeners = new ArrayList<ServerInfoRequestListener>();
        
        module.addServerInfoRequestListeners(serverInfoRequestListeners);
        
        Assert.assertEquals(1, serverInfoRequestListeners.size());
        
        List<InfoElement> infoElements = serverInfoRequestListeners.get(0).getServerInfosFor(new InfoRequest(null, null, null, null));
        
        Assert.assertEquals(1, infoElements.size());
        Assert.assertTrue(infoElements.get(0) instanceof Feature);
        Assert.assertEquals(NamespaceURIs.PRIVATE_DATA, ((Feature)infoElements.get(0)).getVar());
    }

    @Test
    public void getServerInfosForWithNode() throws ServiceDiscoveryRequestException {
        List<ServerInfoRequestListener> serverInfoRequestListeners = new ArrayList<ServerInfoRequestListener>();
        module.addServerInfoRequestListeners(serverInfoRequestListeners);
        Assert.assertEquals(1, serverInfoRequestListeners.size());
        
        Assert.assertNull(serverInfoRequestListeners.get(0).getServerInfosFor(new InfoRequest(null, null, "node", null)));
    }
    
    @Test
    public void addHandlerDictionaries() {
        List<HandlerDictionary> dictionaries = new ArrayList<HandlerDictionary>();
        
        module.addHandlerDictionaries(dictionaries);
        
        Assert.assertEquals(1, dictionaries.size());
        Assert.assertTrue(dictionaries.get(0) instanceof NamespaceHandlerDictionary);
        NamespaceHandlerDictionary namespaceHandlerDictionary = (NamespaceHandlerDictionary) dictionaries.get(0);
        
        Assert.assertEquals(NamespaceURIs.PRIVATE_DATA, namespaceHandlerDictionary.getNamespaceURI());
    }
}
