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
package org.apache.vysper.spring;

import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.XMPPServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * this class is able to boot a standalone XMPP server in a spring context.
 * See the example spring-context.xml for details
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class SpringCompatibleXMPPServer extends XMPPServer {

    protected final List<Module> listOfModules = new ArrayList<Module>();
    protected String certificateFile = null;
    protected String certificatePassword = null;

    public SpringCompatibleXMPPServer(String domain) {
        super(domain);
    }

    public void setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public void setEndpoints(Collection<Endpoint> endpoints) {
        for (Endpoint endpoint : endpoints) {
            addEndpoint(endpoint);
        }
    }

    public void setModules(Collection<Module> modules) {
        listOfModules.addAll(modules);
    }
    
    public void init() throws Exception {
        setTLSCertificateInfo(new File(certificateFile), certificatePassword);
        start();
        if (listOfModules != null) {
            for (Module module : listOfModules) {
                addModule(module);
            }
        }
    }

    public void destroy() {
        stop();
    }
}
