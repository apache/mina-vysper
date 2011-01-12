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

import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormEncoder;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
import org.apache.vysper.xmpp.uuid.JVMBuiltinUUIDGenerator;
import org.apache.vysper.xmpp.uuid.UUIDGenerator;

/**
 */
public abstract class AbstractAdhocCommandHandler implements AdhocCommandHandler {
    
    private static UUIDGenerator SESSION_ID_GENERATOR = new JVMBuiltinUUIDGenerator();
    protected static final DataFormEncoder DATA_FORM_ENCODER = new DataFormEncoder();
    
    protected boolean isExecuting = true;
    protected boolean isPrevAllowed = false;
    protected boolean isNextAllowed = false;
    protected String sessionId = SESSION_ID_GENERATOR.create();

    public boolean isExecuting() {
        return isExecuting;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isPrevAllowed() {
        return isPrevAllowed;
    }

    public boolean isNextAllowed() {
        return isNextAllowed;
    }

    protected void addFormTypeField(DataForm dataForm) {
        dataForm.addField(new Field("", Field.Type.HIDDEN, Field.FORM_TYPE, NamespaceURIs.XEP0133_SERVICE_ADMIN));
    }

    protected DataForm createFormForm(final String title, final String instruction) {
        final DataForm dataForm = new DataForm();
        dataForm.setTitle(title);
        dataForm.addInstruction(instruction);
        dataForm.setType(DataForm.Type.form);
        addFormTypeField(dataForm);
        return dataForm;
    }

    protected DataForm createResultForm() {
        final DataForm dataForm = new DataForm();
        dataForm.setType(DataForm.Type.result);
        addFormTypeField(dataForm);
        return dataForm;
    }
}
