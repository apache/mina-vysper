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

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class MAMInfoRequestListenerTest extends IntegrationTest {

    @Test
    public void discoverInfo() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(alice());
        DiscoverInfo discoverInfo = discoveryManager.discoverInfo(alice().getUser().asBareJid());
        List<DiscoverInfo.Feature> features = discoverInfo.getFeatures();

        assertTrue(containsFeature(features, NamespaceURIs.XEP0359_STANZA_IDS));
        assertTrue(containsFeature(features, NamespaceURIs.JABBER_X_DATA));
    }

    private boolean containsFeature(List<DiscoverInfo.Feature> features, String featureUri) {
        return features.stream().map(DiscoverInfo.Feature::getVar).anyMatch(featureUri::equals);
    }

}
