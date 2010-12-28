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
package org.apache.vysper.xmpp.modules.extension.xep0202_entity_time;

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

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class EntityTimeModule extends DefaultDiscoAwareModule implements ServerInfoRequestListener {

    protected boolean supportXEP0090 = true;

    @Override
    public String getName() {
        return "XEP-0202 Entity Time";
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
        infoElements.add(new Feature(NamespaceURIs.URN_XMPP_TIME));
        if (supportXEP0090) {
            infoElements.add(new Feature(NamespaceURIs.JABBER_IQ_TIME));
        }
        return infoElements;
    }

    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.URN_XMPP_TIME, new EntityTimeIQHandler()));
        // backward compatibility to XEP-0090
        if (supportXEP0090)
            dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.JABBER_IQ_TIME,
                    new EntityTimeXEP0090IQHandler()));
    }
}
