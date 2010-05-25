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
package org.apache.vysper.xmpp.extension.xep0124;

import java.io.IOException;

import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows HTTP clients to communicate through the Bosh protocol (http://xmpp.org/extensions/xep-0124.html)
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshEndpoint implements Endpoint {
	
	private final Logger logger = LoggerFactory.getLogger(BoshEndpoint.class);

    private ServerRuntimeContext serverRuntimeContext;

    private int port = 8080;

    private Server server;
    
    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public void setSslEnabled(boolean value) {
    	// TODO:
    }
    
    public void start() throws IOException {
    	server = new Server(port);
    	BoshIoHandler boshIoHandler = new BoshIoHandler();
    	boshIoHandler.setServerRuntimeContext(serverRuntimeContext);
    	server.setHandler(boshIoHandler); 
    	try {
			server.start();
		} catch (Exception e) {
			// TODO IOException(Exception) is only Java 1.6, so throwing a RuntimeException for now
			throw new RuntimeException(e);
		}
    }

    public void stop() {
    	try {
			server.stop();
		} catch (Exception e) {
			logger.warn("Could not stop the Jetty server", e);
		}
    }

}
