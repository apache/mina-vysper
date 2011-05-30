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
package org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command;

import java.util.List;
import java.util.Map;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.Note;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormParser;
import org.apache.vysper.xmpp.stanza.dataforms.Field;

/**
 */
public class ChangeUserPasswordCommandHandler extends PasswordCheckingCommandHandler {
    
    protected AccountManagement accountManagement;

    /**
     * if not NULL, the user will only be able this JID's (his own) password
     */
    protected Entity constrainedJID;

    public ChangeUserPasswordCommandHandler(AccountManagement accountManagement, Entity constrainedJID) {
        this.accountManagement = accountManagement;
        this.constrainedJID = constrainedJID;
    }

    public XMLElement process(List<XMLElement> commandElements, List<Note> notes) {
        if (commandElements == null || commandElements.size() == 0) {
            return sendForm();
        } else {
            return processForm(commandElements, notes);
        }
    }

    protected XMLElement sendForm() {
        final DataForm dataForm = createFormForm("Changing a User Password", "Fill out this form to change a user&apos;s password.");
        if (constrainedJID == null) {
            dataForm.addField(new Field("The Jabber ID whose password will be changed.", Field.Type.JID_SINGLE, "accountjid"));
        } else {
            dataForm.addField(new Field("The Jabber ID whose password will be changed.", Field.Type.JID_SINGLE, "accountjid", constrainedJID.getFullQualifiedName()));
        }
        dataForm.addField(new Field("The new password for this account", Field.Type.TEXT_PRIVATE, "password"));
        dataForm.addField(new Field("Retype new password", Field.Type.TEXT_PRIVATE, "password-verify"));

        return DATA_FORM_ENCODER.getXML(dataForm);
    }
    
    protected XMLElement processForm(List<XMLElement> commandElements, List<Note> notes) {
        if (commandElements.size() != 1) {
            throw new IllegalStateException("must be an X element");
        }
        final DataFormParser dataFormParser = new DataFormParser(commandElements.get(0));
        final Map<String,Object> valueMap = dataFormParser.extractFieldValues();
        final Entity accountjid;
        if(valueMap.get("accountjid") instanceof Entity) {
            accountjid = (Entity) valueMap.get("accountjid");
        } else if(valueMap.get("accountjid") != null) {
            accountjid = EntityImpl.parseUnchecked((String) valueMap.get("accountjid"));
        } else {
            accountjid = null;
        }
        final String password = (String)valueMap.get("password");
        final String password2 = (String)valueMap.get("password-verify");

        final boolean success = checkPassword(notes, accountjid, password, password2);
        if (!success) return sendForm();

        if (constrainedJID != null && !constrainedJID.equals(accountjid)) {
            notes.add(Note.error("password change only allowed for " + constrainedJID.getFullQualifiedName()));
            return sendForm();
        }
        
        try {
            accountManagement.changePassword(accountjid, password);
        } catch (AccountCreationException e) {
            notes.add(Note.error("changing password failed for " + accountjid));
            return sendForm();
        }

        isExecuting = false;
        return null;
    }

}
