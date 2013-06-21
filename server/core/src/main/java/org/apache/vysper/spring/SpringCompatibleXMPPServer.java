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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.springframework.core.io.Resource;

/**
 * this class is able to boot a standalone XMPP server in a spring context.
 * See the example spring-context.xml for details
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class SpringCompatibleXMPPServer extends XMPPServer {

    protected final List<Module> listOfModules = new ArrayList<Module>();
    protected File certificateFile = null;
    protected String certificatePassword = null;
    
    protected boolean enableFederationFeature = false;

    public SpringCompatibleXMPPServer(String domain) {
        super(domain);
    }

    public void setCertificateFile(Resource certificateFile) throws IOException {
        this.certificateFile = certificateFile.getFile();
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

    public void setEnableFederationFeature(boolean enableFederationFeature) {
        this.enableFederationFeature = enableFederationFeature;
    }

    @Override
    protected ServerFeatures createServerFeatures() {
        final ServerFeatures serverFeatures = super.createServerFeatures();
        serverFeatures.setRelayingToFederationServers(enableFederationFeature);
        return serverFeatures;
    }

    public void init() throws Exception {
        setTLSCertificateInfo(certificateFile, certificatePassword);
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
