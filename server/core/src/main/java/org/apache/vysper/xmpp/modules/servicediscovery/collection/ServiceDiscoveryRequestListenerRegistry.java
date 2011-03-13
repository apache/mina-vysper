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
package org.apache.vysper.xmpp.modules.servicediscovery.collection;

import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ComponentInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;

/**
 * registers listeners which can react to service discovery item or info requests by yielding features, identities
 * or items to be included in the response
 * @see org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule#getServerInfosFor(org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest)
 * for a simple example how to deal with such a request
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface ServiceDiscoveryRequestListenerRegistry extends ServerRuntimeContextService {

    public static final String SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY = "discoRequestListenerRegistry";

    void addInfoRequestListener(InfoRequestListener infoRequestListener);

    void addServerInfoRequestListener(ServerInfoRequestListener infoRequestListener);

    void addComponentInfoRequestListener(ComponentInfoRequestListener infoRequestListener);

    void addItemRequestListener(ItemRequestListener itemRequestListener);
}
