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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.modules.DefaultModule;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandSupport;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandsModule;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandsService;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.CommandInfo;
import org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command.AddUserCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command.ChangeUserPasswordCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command.GetOnlineUsersCommandHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0133.html">XEP-0133 Service Administration</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServiceAdministrationModule extends DefaultModule implements AdhocCommandSupport, ServerRuntimeContextService, ServerAdministrationService {

    private final Logger logger = LoggerFactory.getLogger(ServiceAdministrationModule.class);

    public static final String COMMAND_NODE_ADD_USER = "http://jabber.org/protocol/admin#add-user";
    public static final String COMMAND_GET_ONLINE_USERS_NUM = "http://jabber.org/protocol/admin#get-online-users-num";
    public static final String COMMAND_CHANGE_USER_PASSWORD = "http://jabber.org/protocol/admin#change-user-password";
    
    private ServerRuntimeContext serverRuntimeContext;

    protected Collection<Entity> admins = new HashSet<Entity>();
    protected final Map<String, CommandInfo> allCommandInfos = new HashMap<String, CommandInfo>();

    public ServiceAdministrationModule() {
        /* XEP-133 4.1  */ allCommandInfos.put(COMMAND_NODE_ADD_USER, new CommandInfo(COMMAND_NODE_ADD_USER, "Add User"));
        /* XEP-133 4.7  */ allCommandInfos.put(COMMAND_CHANGE_USER_PASSWORD, new CommandInfo(COMMAND_CHANGE_USER_PASSWORD, "Change User Password"));
        /* XEP-133 4.15 */ allCommandInfos.put(COMMAND_GET_ONLINE_USERS_NUM, new CommandInfo(COMMAND_GET_ONLINE_USERS_NUM, "Get Number of Online Users"));
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
        
        serverRuntimeContext.registerServerRuntimeContextService(this);
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

    public boolean isAdmin(Entity adminCandidate) {
        return admins.contains(adminCandidate);
    }

    @Override
    public String getName() {
        return "XEP-0133 Service Administration";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    public String getServiceName() {
        return ServerAdministrationService.SERVICE_ID;
    }
    
    public Collection<CommandInfo> getCommandInfosForInfoRequest(InfoRequest infoRequest, boolean hintListAll) {
        if (!admins.contains(infoRequest.getFrom())) {
            return Arrays.asList(allCommandInfos.get(COMMAND_CHANGE_USER_PASSWORD));
        }
        if (hintListAll) return allCommandInfos.values();

        final CommandInfo commandInfo = allCommandInfos.get(infoRequest.getNode());
        return (commandInfo == null) ? null : Arrays.asList(commandInfo);
    }

    public AdhocCommandHandler getCommandHandler(String commandNode, Entity executingUser) {
        if (executingUser == null) return null;

        final AccountManagement accountManagement = serverRuntimeContext.getStorageProvider(AccountManagement.class);
        final ResourceRegistry resourceRegistry = serverRuntimeContext.getResourceRegistry();
        
        if (!admins.contains(executingUser.getBareJID())) {
            // non-admins can only admin their own accounts
            if (commandNode.equals(COMMAND_CHANGE_USER_PASSWORD)) {
                return new ChangeUserPasswordCommandHandler(accountManagement, executingUser);
            }
            return null;
        }
        
        if (commandNode.equals(COMMAND_NODE_ADD_USER)) {
            if (accountManagement == null) return null;
            return new AddUserCommandHandler(accountManagement, Arrays.asList(serverRuntimeContext.getServerEntity().getDomain()));
        } else if (commandNode.equals(COMMAND_CHANGE_USER_PASSWORD)) {
            return new ChangeUserPasswordCommandHandler(accountManagement, null);
        } else if (commandNode.equals(COMMAND_GET_ONLINE_USERS_NUM)) {
            return new GetOnlineUsersCommandHandler(resourceRegistry);
        }
        return null;
    }
}
