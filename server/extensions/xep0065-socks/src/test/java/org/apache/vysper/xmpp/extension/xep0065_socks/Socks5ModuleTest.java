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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.components.ComponentStanzaProcessor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Socks5ModuleTest extends Mockito {

    private static final Entity SERVER = EntityImpl.parseUnchecked("vysper.org");

    private static final Entity COMPONENT = EntityImpl.parseUnchecked("socks.vysper.org");

    private static final Entity FROM = EntityImpl.parseUnchecked("user@vysper.org");

    private Socks5Module module = new Socks5Module("socks");

    private ServerRuntimeContext serverRuntimeContext = mock(ServerRuntimeContext.class);

    private Socks5ConnectionsRegistry connectionsRegistry = mock(Socks5ConnectionsRegistry.class);

    @Before
    public void before() {
        when(serverRuntimeContext.getServerEntity()).thenReturn(SERVER);

        module.setConnectionsRegistry(connectionsRegistry);
    }

    @Test
    public void getName() {
        Assert.assertNotNull(module.getName());
    }

    @Test
    public void getVersion() {
        Assert.assertNotNull(module.getVersion());
    }

    @Test
    public void getSubdomain() {
        Assert.assertEquals("socks", module.getSubdomain());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullSubdomain() {
        new Socks5Module(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptySubdomain() {
        new Socks5Module("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fullDomain() {
        new Socks5Module("socks.vysper.org");
    }

    @Test
    public void discoItems() throws Exception {
        ServiceCollector collector = new ServiceCollector();

        when(serverRuntimeContext.getServerRuntimeContextService(
                ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY))
                        .thenReturn(collector);

        module = new Socks5Module("socks");

        module.initialize(serverRuntimeContext);

        InfoRequest infoRequest = new InfoRequest(FROM, SERVER, null, "id1");
        List<Item> items = collector.processItemRequest(infoRequest, null);

        List<Item> expected = Arrays.asList(new Item(COMPONENT));
        Assert.assertEquals(expected, items);
    }

    @Test
    public void discoComponentInfo() throws Exception {
        ServiceCollector collector = new ServiceCollector();

        when(serverRuntimeContext.getServerRuntimeContextService(
                ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY))
                        .thenReturn(collector);

        module = new Socks5Module("socks");

        module.initialize(serverRuntimeContext);

        InfoRequest infoRequest = new InfoRequest(FROM, COMPONENT, null, "id1");
        List<InfoElement> infoElements = collector.processComponentInfoRequest(infoRequest, null);

        List<InfoElement> expected = Arrays.asList(new Identity("proxy", "bytestreams", "File Transfer Relay"),
                new Feature(NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS));
        Assert.assertEquals(expected, infoElements);
    }

    @Test
    public void proxy() throws Exception {
        int port = findFreePort();

        InetSocketAddress address = new InetSocketAddress(port);

        module = new Socks5Module("socks", address);
        module.initialize(serverRuntimeContext);

        Thread.sleep(200);

        assertSocket(port);
    }

    @Test
    public void proxyDefaultAddress() throws Exception {
        int port = findFreePort();

        module = new Socks5Module("socks", new InetSocketAddress(port));
        module.initialize(serverRuntimeContext);

        Thread.sleep(200);

        assertSocket(port);
    }

    @Test(expected = RuntimeException.class)
    public void proxyAddressInUse() throws Exception {
        int port = findFreePort();

        ServerSocket ss = new ServerSocket(port);

        module = new Socks5Module("socks", new InetSocketAddress(port));
        module.initialize(serverRuntimeContext);

        module.close();
        ss.close();
    }

    private int findFreePort() throws IOException, SocketException {
        ServerSocket ss = new ServerSocket(0);
        ss.setReuseAddress(true);
        int port = ss.getLocalPort();
        ss.close();
        return port;
    }

    private void assertSocket(int port) throws UnknownHostException, IOException {
        Socket socket = new Socket("127.0.0.1", port);
        socket.close();
    }

    @After
    public void after() {
        if (module != null) {
            module.close();
        }
    }

}
