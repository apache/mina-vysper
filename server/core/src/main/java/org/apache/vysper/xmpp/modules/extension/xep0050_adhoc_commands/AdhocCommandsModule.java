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
package org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.DefaultDiscoAwareModule;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Identity;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ItemRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServerInfoRequestListener;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0050.html">XEP-0050 Ad-hoc Commands</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class AdhocCommandsModule extends DefaultDiscoAwareModule 
        implements ItemRequestListener, ServerInfoRequestListener, 
                   ServerRuntimeContextService, AdhocCommandsService {

    public static final String ADHOC_COMMANDS = "adhoc_commands";
    
    private final Logger logger = LoggerFactory.getLogger(org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandsModule.class);

    protected ServerRuntimeContext serverRuntimeContext;

    protected AdhocCommandIQHandler iqHandler;
    
    protected final List<AdhocCommandSupport> adhocCommandSupporters = new ArrayList<AdhocCommandSupport>();

    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);

        this.serverRuntimeContext = serverRuntimeContext;

        serverRuntimeContext.registerServerRuntimeContextService(this);
    }

    @Override
    public String getName() {
        return "XEP-0050 Ad-hoc Commands";
    }

    @Override
    public String getVersion() {
        return "1.2";
    }

    /**
     * Make this object available for disco#items requests.
     */
    @Override
    protected void addItemRequestListeners(List<ItemRequestListener> itemRequestListeners) {
        itemRequestListeners.add(this);
    }

    @Override
    protected void addServerInfoRequestListeners(List<ServerInfoRequestListener> serverInfoRequestListeners) {
        serverInfoRequestListeners.add(this);
    }

    /**
     * Implements the getItemsFor method from the {@link ItemRequestListener} interface.
     * Makes this modules available via disco#items and returns the associated nodes.
     * 
     * @see ItemRequestListener#getItemsFor(InfoRequest, StanzaBroker)
     */
    public List<Item> getItemsFor(InfoRequest request, StanzaBroker stanzaBroker) throws ServiceDiscoveryRequestException {
        if (!NamespaceURIs.XEP0050_ADHOC_COMMANDS.equals(request.getNode())) {
            return null;
        }

        List<CommandInfo> allCommandInfos = new ArrayList<CommandInfo>();
        for (AdhocCommandSupport adhocCommandSupporter : adhocCommandSupporters) {
            final Collection<CommandInfo> commandInfos = adhocCommandSupporter.getCommandInfosForInfoRequest(request, true);
            if (commandInfos != null) allCommandInfos.addAll(commandInfos);
        }
        if (allCommandInfos.size() == 0) return null; // do not announce when no command available

        // formulate info response from collected command infos
        List<Item> items = new ArrayList<Item>();
        for (CommandInfo commandInfo : allCommandInfos) {
            Entity jid = commandInfo.getJid();
            if (jid == null) jid = serverRuntimeContext.getServerEntity();

            String node = commandInfo.getNode();
            if (node == null) {
                logger.warn("no node for command info, ignoring. command name = " + commandInfo.getName());
                continue;
            }

            String name = commandInfo.getName() == null ? commandInfo.getNode() : commandInfo.getName();
            items.add(new Item(jid, name, node));
        }

        return items;
    }

    public List<InfoElement> getServerInfosFor(InfoRequest request) throws ServiceDiscoveryRequestException {
        if (adhocCommandSupporters.size() == 0) return null; // do not announce when no command available

        if (StringUtils.isEmpty(request.getNode())) {
            return Arrays.asList((InfoElement)new Feature(NamespaceURIs.XEP0050_ADHOC_COMMANDS));
        }
        
        // info for specific node has been asked
        List<CommandInfo> allCommandInfos = new ArrayList<CommandInfo>();
        for (AdhocCommandSupport adhocCommandSupporter : adhocCommandSupporters) {
            final Collection<CommandInfo> commandInfos = adhocCommandSupporter.getCommandInfosForInfoRequest(request, false);
            if (commandInfos != null) allCommandInfos.addAll(commandInfos);
        }
        if (allCommandInfos.size() == 0) return null; // do not announce when no command available

        final CommandInfo commandInfo = allCommandInfos.get(0);
        final ArrayList<InfoElement> infoElements = new ArrayList<InfoElement>();
        infoElements.add(new Identity("automation", "command-node", commandInfo.getName()));
        infoElements.add(new Feature(NamespaceURIs.XEP0050_ADHOC_COMMANDS));
        infoElements.add(new Feature(NamespaceURIs.JABBER_X_DATA));
        return infoElements;
        
    }

    public String getServiceName() {
        return ADHOC_COMMANDS;
    }

    public void registerCommandSupport(AdhocCommandSupport adhocCommandSupport) {
        adhocCommandSupporters.add(adhocCommandSupport);
    }

    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        iqHandler = new AdhocCommandIQHandler(Collections.unmodifiableCollection(adhocCommandSupporters));
        dictionary.add(new NamespaceHandlerDictionary(NamespaceURIs.XEP0050_ADHOC_COMMANDS, iqHandler));
    }

}
