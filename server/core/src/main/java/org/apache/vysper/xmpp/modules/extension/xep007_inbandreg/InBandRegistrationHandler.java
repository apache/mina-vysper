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
package org.apache.vysper.xmpp.modules.extension.xep007_inbandreg;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0077.html">XEP-0077 In-Band Registration</a>.
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0077", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class InBandRegistrationHandler extends DefaultIQHandler {

    public InBandRegistrationHandler() {
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.JABBER_IQ_REGISTER);
    }

    @Override
    public boolean isSessionRequired() {
        return false;
    }

    @Override
    protected Stanza handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        //        <iq type='result' id='reg1'>
        //        <query xmlns='jabber:iq:register'>
        //          <instructions>
        //            Choose a username and password for use with this service.
        //            Please also provide your email address.
        //          </instructions>
        //          <username/>
        //          <password/>
        //          <email/>
        //        </query>
        //      </iq>

        if(sessionContext.getState().equals(SessionState.STARTED)
                || sessionContext.getState().equals(SessionState.ENCRYPTED) 
                || sessionContext.getState().equals(SessionState.AUTHENTICATED)) {
            StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(),
                    IQStanzaType.RESULT, stanza.getID());
            stanzaBuilder.startInnerElement("query", NamespaceURIs.JABBER_IQ_REGISTER)
                .startInnerElement("instructions", NamespaceURIs.JABBER_IQ_REGISTER)
                .addText("Choose a username and password for use with this service.")
                .endInnerElement();
                if(sessionContext.getState().equals(SessionState.AUTHENTICATED)) {
                    stanzaBuilder.startInnerElement("registered", NamespaceURIs.JABBER_IQ_REGISTER).endInnerElement()
                        .startInnerElement("username", NamespaceURIs.JABBER_IQ_REGISTER)
                        .addText(sessionContext.getInitiatingEntity().getNode())
                        .endInnerElement();
                } else {
                    stanzaBuilder.startInnerElement("username", NamespaceURIs.JABBER_IQ_REGISTER).endInnerElement()
                        .startInnerElement("password", NamespaceURIs.JABBER_IQ_REGISTER).endInnerElement();
                }
                return stanzaBuilder.build();
        } else {
            return ServerErrorResponses.getStanzaError(StanzaErrorCondition.SERVICE_UNAVAILABLE, stanza, StanzaErrorType.CANCEL, null, null, null);
        }
    }
    
    @Override
    protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        //        <iq type='set' id='reg2'>
        //        <query xmlns='jabber:iq:register'>
        //          <username>bill</username>
        //          <password>Calliope</password>
        //          <email>bard@shakespeare.lit</email>
        //        </query>
        //      </iq>
        
        if(sessionContext.getState().equals(SessionState.STARTED)
                || sessionContext.getState().equals(SessionState.ENCRYPTED)
                || sessionContext.getState().equals(SessionState.AUTHENTICATED)) {

            try {
                XMLElement query = stanza.getSingleInnerElementsNamed("query", NamespaceURIs.JABBER_IQ_REGISTER);
                XMLElement usernameElm = query.getSingleInnerElementsNamed("username", NamespaceURIs.JABBER_IQ_REGISTER);
                if(usernameElm == null || usernameElm.getInnerText() == null) throw new XMLSemanticError("Invalid or missing username");
                String username = usernameElm.getInnerText().getText();
                
                XMLElement passwordElm = query.getSingleInnerElementsNamed("password", NamespaceURIs.JABBER_IQ_REGISTER);
                if(passwordElm == null ||  passwordElm.getInnerText() == null) throw new XMLSemanticError("Invalid or missing password");
                String password = passwordElm.getInnerText().getText();
                if(password.trim().length() == 0) throw new XMLSemanticError("Invalid password");
    
                AccountManagement accountManagement = (AccountManagement) serverRuntimeContext.getStorageProvider(AccountManagement.class);
                Entity user;
                if(username.contains("@")) {
                    user = EntityImpl.parse(username);
                    if(!serverRuntimeContext.getServerEnitity().getDomain().equals(user.getDomain())) {
                        throw new XMLSemanticError("Username must be in the same domain as the server");
                    }
                } else {
                    user = EntityImpl.parse(username + "@" + serverRuntimeContext.getServerEnitity());
                }
                
                if(sessionContext.getState().equals(SessionState.AUTHENTICATED)) {
                    if(accountManagement.verifyAccountExists(user)) {
                        // account exists
                        accountManagement.changePassword(user, password);
                    } else {
                        throw new AccountCreationException("Account does not exist");
                    }
                } else {
                    if(accountManagement.verifyAccountExists(user)) {
                        // account exists
                        throw new AccountCreationException("Account already exists");
                    } else {
                        accountManagement.addUser(user, password);
                    }
                }
                return StanzaBuilder.createDirectReply(stanza, true, IQStanzaType.RESULT).build();
                
            } catch (XMLSemanticError e) {
                //        <iq type='error' id='reg2'>
                //            <query xmlns='jabber:iq:register'>
                //              <username>bill</username>
                //              <password>Calliope</password>
                //            </query>
                //            <error code='406' type='modify'>
                //              <not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
                //            </error>
                //          </iq>
                return ServerErrorResponses.getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE, stanza, StanzaErrorType.MODIFY, 406, null, null, null);
            } catch (EntityFormatException e) {
                return ServerErrorResponses.getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE, stanza, StanzaErrorType.MODIFY, 406, null, null, null);
            } catch (AccountCreationException e) {
                return ServerErrorResponses.getStanzaError(StanzaErrorCondition.CONFLICT, stanza, StanzaErrorType.CANCEL, 409, e.getMessage(), null, null);
            }
        } else {
            return ServerErrorResponses.getStanzaError(StanzaErrorCondition.SERVICE_UNAVAILABLE, stanza, StanzaErrorType.CANCEL, null, null, null);
        }
    }
  
    
}
