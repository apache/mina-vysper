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
package org.apache.vysper.console;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;


/**
 * Standalone admin console. Defaults to listening on port 8222.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class AdminConsole {

    private int port = 8222;
    private Server server;

    /**
     * Start the admin console in an embedded web server
     * @throws Exception
     */
    public void start() throws Exception {
        server = new Server(port);
        
        WebAppContext context = new WebAppContext();
        context.setDescriptor("src/main/resources/webapp/WEB-INF/web.xml");
        context.setResourceBase("src/main/resources/webapp");
        context.setContextPath("/");
 
        server.setHandler(context);
        
        server.start();
    }
    
    /**
     * Stop the admin console
     * @throws Exception
     */
    public void stop() throws Exception {
        server.stop();
    }

    /**
     * Get the port on which the admin console will listen. Defaults to 8222.
     * @return The port
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port on which the admin console will listen.
     * @param port The port
     */
    public void setPort(int port) {
        this.port = port;
    }
}
