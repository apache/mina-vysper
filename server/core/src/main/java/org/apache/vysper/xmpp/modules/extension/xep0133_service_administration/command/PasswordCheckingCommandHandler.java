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

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AbstractAdhocCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.Note;

/**
 */
public abstract class PasswordCheckingCommandHandler extends AbstractAdhocCommandHandler {
    protected boolean checkPassword(List<Note> notes, Entity accountjid, String password, String password2) {
        if (StringUtils.isBlank(password) || 
            password.equals(accountjid.getFullQualifiedName()) ||
            password.length() < 8) {
            notes.add(Note.error("password must have at least 8 chars and must not be the same as the new JID"));
            return false;
        }
        if (!password.equals(password2)) {
            notes.add(Note.error("passwords did not match"));
            return false;
        }
        return true;
    }
}
