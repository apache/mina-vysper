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
package org.apache.vysper.xmpp.stanza.dataforms;

import static org.apache.vysper.xmpp.stanza.dataforms.Field.Type.HIDDEN;
import junit.framework.TestCase;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm.Type;

/**
 */
public class DataFormEncoderTestCase extends TestCase {

    public void testXDataForm() throws XMLSemanticError {
        DataForm form = new DataForm();
        form.setTitle("ftitle");
        form.setType(Type.form);
        form.addInstruction("instruction1");
        Field field = new Field("label", HIDDEN, "var");
        field.addValue("val1");
        field.addOption(new Option("1.", "uno"));
        field.addOption(new Option("2.", "due"));
        form.addField(field);

        XMLElement formElement = new DataFormEncoder().getXML(form);
        XMLElementVerifier formElementVerifier = formElement.getVerifier();
        formElementVerifier.nameEquals("x");
        formElementVerifier.attributeEquals("type", "form");
        formElementVerifier.subElementsPresentExact(3);
        formElementVerifier.subElementPresent("title");
        formElementVerifier.subElementPresent("instructions");
        formElementVerifier.subElementPresent("field");

        XMLElement fieldElement = formElement.getSingleInnerElementsNamed("field");
        XMLElementVerifier fieldElementVerifier = fieldElement.getVerifier();
        fieldElementVerifier.subElementsPresentExact(3);
        formElementVerifier.subElementPresent("value");
        formElementVerifier.subElementPresent("option");

    }

    public void testXDataCancel() {
        DataForm form = new DataForm();
        form.setType(Type.cancel);
        form.addField(new Field("label", HIDDEN, "var"));

        XMLElement formElement = new DataFormEncoder().getXML(form);
        assertTrue(formElement.getInnerElements().isEmpty());

    }

    // TODO test 'item' subelements together with 'reported' 
}
