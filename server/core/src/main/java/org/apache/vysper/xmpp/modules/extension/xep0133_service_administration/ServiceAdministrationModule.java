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
package org.apache.vysper.xmpp.modules.extension.xep0133_service_administration;

import org.apache.vysper.storage.StorageProvider;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.authorization.UserAuthorization;
import org.apache.vysper.xmpp.modules.DefaultModule;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandSupport;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandsModule;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandsService;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.CommandInfo;
import org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command.AddUserCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command.GetOnlineUsersCommandHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0133.html">XEP-0133 Service Administration</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServiceAdministrationModule extends DefaultModule implements AdhocCommandSupport {

    private final Logger logger = LoggerFactory.getLogger(ServiceAdministrationModule.class);

    public static final String COMMAND_NODE_ADD_USER = "http://jabber.org/protocol/admin#add-user";
    public static final String COMMAND_GET_ONLINE_USERS_NUM = "http://jabber.org/protocol/admin#get-online-users-num";
    
    private ServerRuntimeContext serverRuntimeContext;

    protected Collection<Entity> admins = new HashSet<Entity>();
    protected final Map<String, CommandInfo> allCommandInfos = new HashMap<String, CommandInfo>();

    public ServiceAdministrationModule() {
        allCommandInfos.put(COMMAND_NODE_ADD_USER, new CommandInfo(COMMAND_NODE_ADD_USER, "Add User"));
        allCommandInfos.put(COMMAND_GET_ONLINE_USERS_NUM, new CommandInfo(COMMAND_GET_ONLINE_USERS_NUM, "Get Number of Online Users"));
    }

    /**
     * Initializes the MUC module, configuring the storage providers.
     */
    @Override
    public void initialize(ServerRuntimeContext serverRuntimeContext) {
        super.initialize(serverRuntimeContext);

        this.serverRuntimeContext = serverRuntimeContext;

        final AdhocCommandsService adhocCommandsService = (AdhocCommandsService)serverRuntimeContext.getServerRuntimeContextService(AdhocCommandsModule.ADHOC_COMMANDS);
        adhocCommandsService.registerCommandSupport(this);
    }

    public void setAddAdminJIDs(Collection<Entity> admins) {
        this.admins.addAll(admins);
    }
    
    public void setAddAdmins(Collection<String> admins) {

        Set<Entity> adminEntities = new HashSet<Entity>();
        for (String admin : admins) {
            try {
                adminEntities.add(EntityImpl.parse(admin));
            } catch (EntityFormatException e) {
                logger.error("could not add mal-formed JID as administrator: " + admin);
            }
        }
        this.admins.addAll(adminEntities);
    }
    
    @Override
    public String getName() {
        return "XEP-0133 Service Administration";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    public Collection<CommandInfo> getCommandInfosForInfoRequest(InfoRequest infoRequest, boolean hintListAll) {
        if (!admins.contains(infoRequest.getFrom())) {
            return null;
        }
        if (hintListAll) return allCommandInfos.values();

        final CommandInfo commandInfo = allCommandInfos.get(infoRequest.getNode());
        return (commandInfo == null) ? null : Arrays.asList(commandInfo);
    }

    public AdhocCommandHandler getCommandHandler(String commandNode, Entity executingUser) {
        if (executingUser == null || !admins.contains(executingUser)) {
            return null;
        }
        if (commandNode.equals(COMMAND_NODE_ADD_USER)) {
            final AccountManagement accountManagement = (AccountManagement)serverRuntimeContext.getStorageProvider(AccountManagement.class);
            if (accountManagement == null) return null;
            return new AddUserCommandHandler(accountManagement, Arrays.asList(serverRuntimeContext.getServerEnitity().getDomain()));
        } else if (commandNode.equals(COMMAND_GET_ONLINE_USERS_NUM)) {
            return new GetOnlineUsersCommandHandler(serverRuntimeContext.getResourceRegistry());
        }
        return null;
    }
}
