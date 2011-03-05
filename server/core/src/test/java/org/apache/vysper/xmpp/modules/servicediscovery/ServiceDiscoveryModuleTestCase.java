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
package org.apache.vysper.xmpp.modules.servicediscovery;

import junit.framework.Assert;

import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.junit.Test;

/**
 */
public class ServiceDiscoveryModuleTestCase {

    private ServiceDiscoveryModule module = new ServiceDiscoveryModule();
    
    @Test
    public void nameMustBeProvided() {
        Assert.assertNotNull(module.getName());
    }

    @Test
    public void versionMustBeProvided() {
        Assert.assertNotNull(module.getVersion());
    }
    
    @Test
    public void getHandlerDictionaries() throws ServiceDiscoveryRequestException {
        Assert.assertEquals(2, module.getHandlerDictionaries().size());
        Assert.assertTrue(module.getHandlerDictionaries().get(0) instanceof ServiceDiscoveryInfoDictionary);
        Assert.assertTrue(module.getHandlerDictionaries().get(1) instanceof ServiceDiscoveryItemDictionary);
    }

    @Test
    public void getServerServices() throws ServiceDiscoveryRequestException {
        Assert.assertEquals(1, module.getServerServices().size());
        Assert.assertTrue(module.getServerServices().get(0) instanceof ServiceCollector);
    }

}
