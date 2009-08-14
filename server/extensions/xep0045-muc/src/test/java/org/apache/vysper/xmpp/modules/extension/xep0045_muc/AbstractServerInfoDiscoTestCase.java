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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractServerInfoDiscoTestCase extends AbstractInfoDiscoTestCase {

    protected ServerInfoRequestListener getServerInfoRequestListener() {
        Module module = getModule();
        if(module instanceof ServerInfoRequestListener) {
            return (ServerInfoRequestListener) module;
        } else {
            throw new RuntimeException("Module does not implement ServerInfoRequestListener");
        }
    }
    
    protected Entity getTo() {
        return SERVER_JID;
    }

    
    @Override
    protected void addListener(ServiceCollector serviceCollector) {
        serviceCollector.addServerInfoRequestListener(getServerInfoRequestListener());
    }
}
