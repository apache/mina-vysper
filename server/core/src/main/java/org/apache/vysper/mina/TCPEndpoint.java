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
package org.apache.vysper.mina;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.vysper.mina.codec.XMPPProtocolCodecFactory;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class TCPEndpoint implements Endpoint {

    private ServerRuntimeContext serverRuntimeContext;
    
    private StanzaProcessor stanzaProcessor;

    private int port = 5222;

    private SocketAcceptor acceptor;
    
    /**
     * @deprecated Use {@link C2SEndpoint} or {@link S2SEndpoint} instead. This class will
     *          be made abstract in a future release.
     */
    public TCPEndpoint() {
    }

    protected TCPEndpoint(int port) {
        this.port = port;
    }

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    @Override
    public void setStanzaProcessor(StanzaProcessor stanzaProcessor) {
        this.stanzaProcessor = stanzaProcessor;
    }

    /**
     * Returns the configured port if one is provided (non-zero value).
     */
    public int getPort() {
        if(port != 0 || acceptor == null) {
            return port;
        } else {
            return acceptor.getLocalAddress().getPort();
        }
    }

    public void setPort(int port) {
        if(acceptor != null) {
            throw new IllegalStateException("Endpoint started, can not set port");
        }
        
        this.port = port;
    }

    public void start() throws IOException {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();

        DefaultIoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
        filterChainBuilder.addLast("xmppCodec", new ProtocolCodecFilter(new XMPPProtocolCodecFactory()));
        filterChainBuilder.addLast("loggingFilter", new StanzaLoggingFilter());
        
        int coreThreadCount = 10;
        int maxThreadCount = 20;
        int threadTimeoutSeconds = 2 * 60;
        filterChainBuilder.addLast("executorFilter", new ExecutorFilter(new OrderedThreadPoolExecutor(coreThreadCount, maxThreadCount, threadTimeoutSeconds, TimeUnit.SECONDS)));
        acceptor.setFilterChainBuilder(filterChainBuilder);

        XmppIoHandlerAdapter adapter = new XmppIoHandlerAdapter(serverRuntimeContext, stanzaProcessor);
        acceptor.setHandler(adapter);

        acceptor.setReuseAddress(true);
        acceptor.bind(new InetSocketAddress(port));

        this.acceptor = acceptor;
    }

    public void stop() {
        acceptor.unbind();
        acceptor.dispose();
    }
}
