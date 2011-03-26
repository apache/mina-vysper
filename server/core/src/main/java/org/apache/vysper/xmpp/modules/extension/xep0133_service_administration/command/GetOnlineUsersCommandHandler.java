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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AbstractAdhocCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.Note;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

/**
 */
public class GetOnlineUsersCommandHandler extends AbstractAdhocCommandHandler {
    
    protected final ResourceRegistry resourceRegistry;

    public GetOnlineUsersCommandHandler(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
    }


    public XMLElement process(List<XMLElement> commandElements, List<Note> notes) {
        final long sessionCount = resourceRegistry.getSessionCount();

        final DataForm dataForm = createResultForm();
        dataForm.addField(new Field("The number of online users", Field.Type.FIXED, "onlineusersnum", Long.toString(sessionCount)));

        isExecuting = false;

        return DATA_FORM_ENCODER.getXML(dataForm);
    }

}
