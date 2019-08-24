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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 */
public class AdhocCommandIQHandler extends DefaultIQHandler {

    protected final Collection<AdhocCommandSupport> adhocCommandSupporters;
    protected final Map<String, AdhocCommandHandler> runningCommands = new HashMap<String, AdhocCommandHandler>();

    public AdhocCommandIQHandler(Collection<AdhocCommandSupport> adhocCommandSupporters) {
        this.adhocCommandSupporters = adhocCommandSupporters;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.XEP0050_ADHOC_COMMANDS);
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "command");
    }

    @Override
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        Entity from = stanza.getFrom();
        if (from == null) {
            from = sessionContext.getInitiatingEntity();
        }

        AdhocCommandHandler commandHandler = null;
        String commandNode;
        String requestedSessionId;
        String action;  // execute | cancel
        List<XMLElement> commandElements = null;
        try {
            XMLElement commandElement = stanza.getSingleInnerElementsNamed("command");
            if (commandElement == null) {
                return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                        StanzaErrorType.MODIFY, "command is missing", null, null));
            }
            commandNode = commandElement.getAttributeValue("node");
            requestedSessionId = commandElement.getAttributeValue("sessionid");
            action = commandElement.getAttributeValue("action");

            if (StringUtils.isEmpty(requestedSessionId)) {
                for (AdhocCommandSupport commandSupport : adhocCommandSupporters) {
                    commandHandler = commandSupport.getCommandHandler(commandNode, from);
                    if (commandHandler != null) {
                        runningCommands.put(commandHandler.getSessionId(), commandHandler);
                        break;
                    }
                }
            } else {
                commandHandler = runningCommands.get(requestedSessionId);
                if (commandHandler == null) {
                    return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                            StanzaErrorType.CANCEL, "command session id not found: " + requestedSessionId, null, null));
                }
            }
            commandElements = commandElement.getInnerElements();
        } catch (XMLSemanticError xmlSemanticError) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                    StanzaErrorType.MODIFY, "command is not well-formed", null, null));
        }

        if ("cancel".equals(action)) {
            runningCommands.remove(requestedSessionId);
            return buildResponse(stanza, from, commandNode, requestedSessionId, "canceled");
        }

        // handle unauthorized access (or command does not exist at all)
        if (commandHandler == null) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.FORBIDDEN, stanza,
                    StanzaErrorType.CANCEL, "command is not available", null, null));
        }

        List<Note> notes = new ArrayList<Note>();
        final XMLElement result = commandHandler.process(commandElements, notes);

        final String sessionId = commandHandler.getSessionId();
        final boolean isExecuting = commandHandler.isExecuting();

        return buildResponse(stanza, from, commandNode, sessionId, 
                                             isExecuting ? "executing" : "completed", result, notes,
                                              commandHandler.isPrevAllowed(), commandHandler.isNextAllowed());
    }

    private List<Stanza> buildResponse(IQStanza stanza, Entity from, String commandNode, String sessionId, 
                                 final String status) {
        return buildResponse(stanza, from, commandNode, sessionId, status, null, null, false, false);
    }
    
    private List<Stanza> buildResponse(IQStanza stanza, Entity from, String commandNode, String sessionId,
                                 final String status, XMLElement result,
                                 List<Note> notes, boolean isPrevAllowed, boolean isNextAllowed) {
        final StanzaBuilder iqStanza = StanzaBuilder.createIQStanza(null, from, IQStanzaType.RESULT, stanza.getID());
        iqStanza.startInnerElement("command");
        iqStanza.declareNamespace("", NamespaceURIs.XEP0050_ADHOC_COMMANDS);
        iqStanza.addAttribute("node", commandNode);
        iqStanza.addAttribute("sessionid", sessionId);
        iqStanza.addAttribute("status", status);
        if (notes != null && notes.size() > 0) {
            for (Note note : notes) {
                iqStanza.startInnerElement("note");
                iqStanza.addAttribute("type", note.getType().name());
                if (note.getText() != null) iqStanza.addText(note.getText());
                iqStanza.endInnerElement();
            }
        }
        if (isNextAllowed || isPrevAllowed) {
            iqStanza.startInnerElement("action");
            if (isPrevAllowed) iqStanza.startInnerElement("prev").endInnerElement();
            if (isNextAllowed) iqStanza.startInnerElement("next").endInnerElement();
            iqStanza.endInnerElement();
        }
        if (result != null) {
            iqStanza.addPreparedElement(result);
        }
        iqStanza.endInnerElement();

        return Collections.singletonList(iqStanza.build());
    }
/*
<iq from='shakespeare.lit'
    id='add-user-1'
    to='bard@shakespeare.lit/globe'
    type='result'
    xml:lang='en'>
  <command xmlns='http://jabber.org/protocol/commands' 
           node='http://jabber.org/protocol/admin#add-user'
           sessionid='add-user:20040408T0337Z'
           status='executing'>
    <x xmlns='jabber:x:data' type='form'>
      <title>Adding a User</title>
      <instructions>Fill out this form to add a user.</instructions>
      <field type='hidden' var='FORM_TYPE'>
        <value>http://jabber.org/protocol/admin</value>
      </field>
      <field label='The Jabber ID for the account to be added'
             type='jid-single'
             var='accountjid'>
        <required/>
      </field>
      <field label='The password for this account'
             type='text-private'
             var='password'/>
      <field label='Retype password'
             type='text-private'
             var='password-verify'/>
      <field label='Email address'
             type='text-single'
             var='email'/>
      <field label='Given name'
             type='text-single'
             var='given_name'/>
      <field label='Family name'
             type='text-single'
             var='surname'/>
    </x>
  </command>
</iq>
    
     */
    

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {

        Entity to = stanza.getTo();
        Entity from = stanza.getFrom();

        if (from == null) {
            from = sessionContext.getInitiatingEntity();
        }

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(),
                IQStanzaType.RESULT, stanza.getID());
        return Collections.singletonList(stanzaBuilder.build());
    }
}