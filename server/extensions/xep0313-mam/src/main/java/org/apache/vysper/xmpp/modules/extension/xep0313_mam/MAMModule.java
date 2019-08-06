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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.interceptor.MAMStanzaHandlerInterceptor;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.query.MAMIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchives;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 * A module for <a href="https://xmpp.org/extensions/xep-0313.html">XEP-0313
 * Message Archive Management</a>
 * 
 * @author Réda Housni Alaoui
 */
public class MAMModule extends DefaultDiscoAwareModule implements ServerInfoRequestListener {

    private static final String NAMESPACE_V1 = "urn:xmpp:mam:1";

    private static final String NAMESPACE_V2 = "urn:xmpp:mam:2";

    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);

        requireNonNull(serverRuntimeContext.getStorageProvider(MessageArchives.class),
                "Could not find an instance of " + MessageArchives.class);
    }

    @Override
    public String getName() {
        return "XEP-0313 Message Archive Management";
    }

    @Override
    public String getVersion() {
        return "0.6.3";
    }

    @Override
    protected void addServerInfoRequestListeners(List<ServerInfoRequestListener> serverInfoRequestListeners) {
        serverInfoRequestListeners.add(this);
    }

    @Override
    public List<InfoElement> getServerInfosFor(InfoRequest request) {
        if (StringUtils.isNotEmpty(request.getNode())) {
            return Collections.emptyList();
        }

        List<InfoElement> infoElements = new ArrayList<>();
        infoElements.add(new Feature(NAMESPACE_V1));
        infoElements.add(new Feature(NAMESPACE_V2));
        infoElements.add(new Feature(NamespaceURIs.XEP0359_STANZA_IDS));
        infoElements.add(new Feature(NamespaceURIs.JABBER_X_DATA));
        return infoElements;
    }

    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        dictionary.add(new NamespaceHandlerDictionary(NAMESPACE_V1, new MAMIQHandler(NAMESPACE_V1)));
        dictionary.add(new NamespaceHandlerDictionary(NAMESPACE_V2, new MAMIQHandler(NAMESPACE_V2)));
    }

    @Override
    public List<StanzaHandlerInterceptor> getStanzaHandlerInterceptors() {
        return Collections.singletonList(new MAMStanzaHandlerInterceptor());
    }
}
