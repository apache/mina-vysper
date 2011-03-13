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
package org.apache.vysper.xmpp.modules.servicediscovery.management;

import org.apache.vysper.StanzaAssert;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
import org.apache.vysper.xmpp.stanza.dataforms.Field.Type;
import org.junit.Before;
import org.junit.Test;


/**
 */
public class InfoDataFormTestCase {

    private DataForm form = new DataForm();
    
    @Before
    public void before() {
        form.setType(org.apache.vysper.xmpp.stanza.dataforms.DataForm.Type.submit);
        form.addField(new Field("l", Type.TEXT_SINGLE, "v"));
    }

    @Test
    public void insertElement() {
        StanzaBuilder stanzaBuilder = new StanzaBuilder("test");
        
        InfoDataForm infoDataForm = new InfoDataForm(form);
        infoDataForm.insertElement(stanzaBuilder);
        
        Stanza expected = new StanzaBuilder("test")
            .startInnerElement("x", NamespaceURIs.JABBER_X_DATA)
            .addAttribute("type", "submit")
            .startInnerElement("field", NamespaceURIs.JABBER_X_DATA)
            .addAttribute("type", "text-single")
            .addAttribute("var", "v")
            .addAttribute("label", "l")
            .build();
        
        StanzaAssert.assertEquals(expected, stanzaBuilder.build());
    }
}
