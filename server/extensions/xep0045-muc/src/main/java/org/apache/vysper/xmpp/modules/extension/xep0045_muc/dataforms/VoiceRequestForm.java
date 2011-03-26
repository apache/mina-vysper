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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.dataforms;

import static org.apache.vysper.xmpp.stanza.dataforms.DataForm.Type.submit;
import static org.apache.vysper.xmpp.stanza.dataforms.Field.Type.HIDDEN;
import static org.apache.vysper.xmpp.stanza.dataforms.Field.Type.TEXT_SINGLE;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormEncoder;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
import org.apache.vysper.xmpp.stanza.dataforms.Field.Type;


/**
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class VoiceRequestForm {
    
    private static final String TITLE = "Voice request";
    private static final String INSTRUCTION = "To approve this request for voice, select " + 
      "the \"Grant voice to this person?\" " +
      "checkbox and click OK. To skip this request, " + 
      "click the cancel button.";
    
/*
  <x xmlns='jabber:x:data' type='form'>
    <title>Voice request</title>
    <instructions>
      
    </instructions>
    <field var='FORM_TYPE' type='hidden'>
        <value>http://jabber.org/protocol/muc#request</value>
    </field>
    <field var='muc#role'
           type='text-single'
           label='Requested role'>
      <value>participant</value>
    </field>
    <field var='muc#jid'
           type='text-single'
           label='User ID'>
      <value>hag66@shakespeare.lit/pda</value>
    </field>
    <field var='muc#roomnick'
           type='text-single'
           label='Room Nickname'>
      <value>thirdwitch</value>
    </field>
    <field var='muc#request_allow'
           type='boolean'
           label='Grant voice to this person?'>
      <value>false</value>
    </field>
  </x>
 */
    
    private Entity requestor;
    private String nick;
    
    public VoiceRequestForm(Entity requestor, String nick) {
        this.requestor = requestor;
        this.nick = nick;
    }
    
    public DataForm createForm() {
        DataForm form = new DataForm();
        form.setType(submit);
        form.setTitle(TITLE);
        form.addInstruction(INSTRUCTION);
        form.addField(new Field("FORM_TYPE", HIDDEN, null, NamespaceURIs.XEP0045_MUC_REQUEST));
        form.addField(new Field("muc#jid", TEXT_SINGLE, "User ID", requestor.getFullQualifiedName()));
        form.addField(new Field("muc#roomnick", TEXT_SINGLE, "Room Nickname", nick));
        form.addField(new Field("muc#request_allow", Type.BOOLEAN, "Grant voice to this person?", "false"));
        
        return form;
    }
    
    public XMLElement createFormXML() {
        return new DataFormEncoder().getXML(createForm());
    }
}
