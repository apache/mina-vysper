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

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.vysper.mina.codec.XMPPProtocolCodecFactory;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class TCPEndpoint implements Endpoint {

    private ServerRuntimeContext serverRuntimeContext;

    private int port = 5222;

    private SocketAcceptor acceptor;

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        XmppIoHandlerAdapter adapter = new XmppIoHandlerAdapter();
        adapter.setServerRuntimeContext(serverRuntimeContext);

        XMPPProtocolCodecFactory xmppCodec = new XMPPProtocolCodecFactory();

        DefaultIoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
        filterChainBuilder.addLast("executorFilter", new ExecutorFilter());
        filterChainBuilder.addLast("xmppCodec", new ProtocolCodecFilter(xmppCodec));
        filterChainBuilder.addLast("loggingFilter", new LoggingFilter());

        SocketAcceptorConfig socketAcceptorConfig = new SocketAcceptorConfig();
        socketAcceptorConfig.setFilterChainBuilder(filterChainBuilder);
        socketAcceptorConfig.setReuseAddress(true);

        SocketAcceptor acceptor = new SocketAcceptor();
        acceptor.setDefaultConfig(socketAcceptorConfig);
        acceptor.bind(new InetSocketAddress(port), adapter);
        this.acceptor = acceptor;
    }

    public void stop() {
        acceptor.unbindAll();
    }
}
