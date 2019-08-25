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
package org.apache.vysper.xmpp.extension.xep0065_socks;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ComponentInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.components.ComponentStanzaProcessor;


/**
 * Implementation of XEO-0065 SOCKS5 Bytestreams {@link http://xmpp.org/extensions/xep-0065.html}.
 * Will start a SOCKS5 proxy and support the required disco elements.
 *
 * <p>
 * A subdomain must be provided, for example "socks". The proxy address can be customized. Be default, the proxy address
 * would be the full module domain, e.g. socks.vysper.org and port 5777 and will listen on all interfaces. 
 * </p>
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Socks5Module extends DefaultDiscoAwareModule implements Component, ComponentInfoRequestListener, ItemRequestListener {

    private static final String DEFAULT_SUBDOMAIN = "socks";
    private static final int DEFAULT_PORT = 5777;
    private static final int DEFAULT_IDLE_TIME = 120;
    
    private String subdomain;
    private Entity fullDomain;
    private InetSocketAddress proxyAddress = new InetSocketAddress(DEFAULT_PORT);
    private int idleTimeInSeconds = DEFAULT_IDLE_TIME;
    
    private Socks5ConnectionsRegistry connectionsRegistry = new DefaultSocks5ConnectionsRegistry();
    
    /*
    <identity category='proxy'
      type='bytestreams'
      name='File Transfer Relay'/>
    <feature var='http://jabber.org/protocol/bytestreams'/>
     */
    private static final List<InfoElement> COMPONENT_INFO = Arrays.asList(
            new Identity("proxy", "bytestreams", "File Transfer Relay"), 
            new Feature(NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS));
    
    private NioSocketAcceptor acceptor;
    
    /**
     * Constructs a SOCK5 module
     * @param subdomain The subdomain for this component, must be only the subdomain, e.g. "socks"
     * @param proxyAddress The address on which the proxy will listen to SOCKS5 requests
     */
    public Socks5Module(String subdomain, InetSocketAddress proxyAddress) {
        Validate.notEmpty(subdomain, "subdomain can not be empty");
        Validate.isTrue(!subdomain.contains("."), "subdomain should only contain a subdomain name, not the full domain name");
        
        this.subdomain = subdomain;
        if(proxyAddress != null) {
            this.proxyAddress = proxyAddress;
        }
    }

    /**
     * Constructs a SOCK5 module with the proxy listening on the default address
     * @param subdomain The subdomain for this component, must be only the subdomain, e.g. "socks"
     */
    public Socks5Module(String subdomain) {
        this(subdomain, null);
    }
    
    /**
     * Constructs a SOCK5 module with the default subdomain "socks" and the proxy listening on the default address
     */
    public Socks5Module() {
        this(DEFAULT_SUBDOMAIN, null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);
        
        fullDomain = EntityUtils.createComponentDomain(subdomain, serverRuntimeContext);
        
        try {
            startProxy();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start SOCKS5 proxy", e);
        }
    }
    
    public void close() {
        if(acceptor != null) {
            acceptor.unbind();
            acceptor.dispose();
        }
    }

    private void startProxy() throws Exception {
        acceptor = new NioSocketAcceptor();
        acceptor.setHandler(new Socks5AcceptorHandler(connectionsRegistry));
        acceptor.getSessionConfig().setBothIdleTime(idleTimeInSeconds);
        acceptor.bind(proxyAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "SOCKS5 Bytestreams";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return "1.8rc1";
    }

    /**
     * {@inheritDoc}
     */
    public String getSubdomain() {
        return subdomain;
    }

    @Override
    public List<StanzaHandler> getComponentHandlers(Entity fullDomain) {
        return Collections.singletonList(new Socks5IqHandler(fullDomain, proxyAddress, connectionsRegistry));
    }

    @Override
    public List<NamespaceHandlerDictionary> getComponentHandlerDictionnaries(Entity fullDomain) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addItemRequestListeners(List<ItemRequestListener> itemRequestListeners) {
        itemRequestListeners.add(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addComponentInfoRequestListeners(List<ComponentInfoRequestListener> componentInfoRequestListeners) {
        componentInfoRequestListeners.add(this);
    }
    
    /**
     * {@inheritDoc}
     */
    public List<Item> getItemsFor(InfoRequest request, StanzaBroker stanzaBroker) throws ServiceDiscoveryRequestException {
        List<Item> componentItem = new ArrayList<Item>();
        componentItem.add(new Item(fullDomain));
        return componentItem;
    }

    /**
     * {@inheritDoc}
     */
    public List<InfoElement> getComponentInfosFor(InfoRequest request, StanzaBroker stanzaBroker) throws ServiceDiscoveryRequestException {
        return COMPONENT_INFO;
    }

    public void setConnectionsRegistry(Socks5ConnectionsRegistry connectionsRegistry) {
        this.connectionsRegistry = connectionsRegistry;
    }
}
