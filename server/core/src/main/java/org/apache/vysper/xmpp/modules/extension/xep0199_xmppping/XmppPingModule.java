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
package org.apache.vysper.xmpp.modules.extension.xep0199_xmppping;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.protocol.DefaultHandlerDictionary;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.writer.StanzaWriter;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0199.html">XEP-0199 XMPP Ping</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XmppPingModule extends DefaultDiscoAwareModule implements ServerInfoRequestListener {

    private XmppPingIQHandler handler = new XmppPingIQHandler();
    
    @Override
    public String getName() {
        return "XEP-0199 XMPP Ping";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    protected void addServerInfoRequestListeners(List<ServerInfoRequestListener> serverInfoRequestListeners) {
        serverInfoRequestListeners.add(this);
    }

    public List<InfoElement> getServerInfosFor(InfoRequest request) {
        if (StringUtils.isNotEmpty(request.getNode())) return null;

        List<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Feature(NamespaceURIs.URN_XMPP_PING));
        return infoElements;
    }

    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        dictionary.add(new DefaultHandlerDictionary(handler));
    }
    
    public void ping(StanzaWriter stanzaWriter, Entity from, Entity to, int timeoutMillis, XmppPingListener listener) {
        XmppPinger pinger = new XmppPinger(handler);
        pinger.ping(stanzaWriter, from, to, timeoutMillis, listener);
    }
    
    
}
