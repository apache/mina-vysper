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
package org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class VcardTempModule extends DefaultDiscoAwareModule implements ServerInfoRequestListener {

    final Logger logger = LoggerFactory.getLogger(VcardTempModule.class);

    protected VcardTempIQHandler iqHandler = new VcardTempIQHandler();

    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);

        VcardTempPersistenceManager persistenceManager = serverRuntimeContext
                .getStorageProvider(VcardTempPersistenceManager.class);
        if (persistenceManager == null) {
            logger.error("no VcardTempPersistenceManager found");
        } else if (!persistenceManager.isAvailable()) {
            logger.warn("VcardTempPersistenceManager not available");
        } else {
            iqHandler.setPersistenceManager(persistenceManager);
        }
    }

    @Override
    public String getName() {
        return "XEP-0054 Vcard-temp";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    protected void addServerInfoRequestListeners(List<ServerInfoRequestListener> serverInfoRequestListeners) {
        serverInfoRequestListeners.add(this);
    }

    public List<InfoElement> getServerInfosFor(InfoRequest request) {
        if (StringUtils.isNotEmpty(request.getNode())) return null;

        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Feature(NamespaceURIs.VCARD_TEMP));
        return infoElements;
    }

    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        iqHandler = new VcardTempIQHandler();
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.VCARD_TEMP, iqHandler));
    }
}