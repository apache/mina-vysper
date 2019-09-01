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
package org.apache.vysper.xmpp.modules;

import java.util.List;
import java.util.Optional;

import org.apache.vysper.event.EventListenerDictionary;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.StanzaHandlerInterceptor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.XMPPServer;

/**
 * a module plugs new functionality into the server, most probably an implementation for a XEP.
 * this functionality spans:
 * <ul>
 * <li>adding services (implementing ServerRuntimeContextService) to the server which can be accessed by other modules</li>
 * <li>doing initializations, for example adding request listeners to the ServiceDiscoveryRequestListenerRegistry</li>
 * <li>adding dictionaries with new stanza handlers getting registered with the server which then get called as
 *     matching stanzas arrive</li>
 * <li>adding dictionaries with new event listeners getting registered with the server which then get called as
 *     matching events are published</li>
 * </ul>
 *
 * TODO: think about returning the supported XEPs
 *
 * @see org.apache.vysper.xmpp.modules.DefaultModule recommended for simple modules not involved with service disco
 * @see org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule recommended for modules responding to service disco requests
 * @see org.apache.vysper.xmpp.modules.ServerRuntimeContextService
 * @see org.apache.vysper.xmpp.protocol.HandlerDictionary
 * @see EventListenerDictionary
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface Module {

    String getName();

    String getVersion();

    /**
     * all stanza handler dictionaries to be added to the server
     */
    List<HandlerDictionary> getHandlerDictionaries();
    
    List<StanzaHandlerInterceptor> getStanzaHandlerInterceptors();

    /**
     * @return The event listener dictionary to be added to the server
     */
    Optional<EventListenerDictionary> getEventListenerDictionary(); 

    /**
     * all objects to be added to the server runtime context
     */
    List<ServerRuntimeContextService> getServerServices();

    /**
     * allows for the module to do some initialization in the light of the server runtime context
     * it runs in. this can for example be used to hook up with service discovery.
     * the server runtime context calls the initialize() method <i>after</i> the whole list of modules
     * it has received has been processed with respect to HandlerDictionaries and
     * ServerRuntimeContextService. if however, modules come late, e.g. they are added in a second batch
     * this applies for all the second batch modules, but initialize() will not be called again for the
     * first one.
     */
    void initialize(ServerRuntimeContext serverRuntimeContext);
    
    /**
     * Allow for the module to release any resources held, for example database connections. The method
     * will be called by {@link XMPPServer#stop()}.
     */
    void close();
}
