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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * makes XMPP out of a Data Form as provided by the model contained in a {@link org.apache.vysper.xmpp.stanza.dataforms.DataForm}
 * instance.
 */
public class DataFormEncoder {

    public XMLElement getXML(DataForm dataForm) {

        List<XMLFragment> childElements = new ArrayList<XMLFragment>();

        Iterator<String> instructionIterator = dataForm.getInstructionIterator();
        while (instructionIterator.hasNext()) {
            String instruction = instructionIterator.next();
            if (instruction == null)
                continue;
            childElements.add(createTextOnlyElement(NamespaceURIs.JABBER_X_DATA, "instructions", instruction));
        }

        if (dataForm.getTitle() != null) {
            childElements.add(createTextOnlyElement(NamespaceURIs.JABBER_X_DATA, "title", dataForm.getTitle()));
        }

        if (dataForm.getType() == DataForm.Type.form) {
            // reported element, containing the reported items
            ArrayList<XMLFragment> reportedFields = new ArrayList<XMLFragment>();
            Iterator<Field> reportedIterator = dataForm.getReportedIterator();
            while (reportedIterator.hasNext()) {
                Field field = reportedIterator.next();
                reportedFields.add(encodeField(field));
            }
            XMLElement reportedElement = new XMLElement(NamespaceURIs.JABBER_X_DATA, "reported", null, null,
                    reportedFields);
            childElements.add(reportedElement);

            // all item elements with their values
            Iterator<List<Field>> itemIterator = dataForm.getItemIterator();
            while (itemIterator.hasNext()) {
                ArrayList<XMLFragment> itemFields = new ArrayList<XMLFragment>();
                List<Field> itemField = itemIterator.next();
                for (Field field : itemField) {
                    itemFields.add(encodeField(field));
                }
                XMLElement itemElement = new XMLElement(NamespaceURIs.JABBER_X_DATA, "item", null, null, itemFields);
                childElements.add(itemElement);
            }
        }

        if (dataForm.getType() != DataForm.Type.cancel) {
            // all fields
            Iterator<Field> fieldIterator = dataForm.getFieldIterator();
            while (fieldIterator.hasNext()) {
                Field field = fieldIterator.next();
                childElements.add(encodeField(field));
            }
        }

        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute("type", dataForm.getType().value()));

        return new XMLElement(NamespaceURIs.JABBER_X_DATA, "x", null, attributes, childElements);
    }

    protected XMLElement encodeField(Field field) {

        ArrayList<XMLFragment> fieldElements = new ArrayList<XMLFragment>();

        List<Attribute> fieldAttributes = new ArrayList<Attribute>();
        if (field.getVar() != null) {
            fieldAttributes.add(new Attribute("var", field.getVar()));
        }
        if (field.getLabel() != null) {
            fieldAttributes.add(new Attribute("label", field.getLabel()));
        }
        if (field.getType() != null) {
            fieldAttributes.add(new Attribute("type", field.getType().value()));
        }

        if (field.getDesc() != null) {
            ArrayList<XMLFragment> descFragment = new ArrayList<XMLFragment>();
            descFragment.add(new XMLText(field.getDesc()));
            fieldElements.add(new XMLElement(NamespaceURIs.JABBER_X_DATA, "desc", null, null, descFragment));
        }

        if (field.isRequired()) {
            fieldElements.add(createEmptyElement(NamespaceURIs.JABBER_X_DATA, "required"));
        }

        Iterator<String> valueIterator = field.getValueIterator();
        while (valueIterator.hasNext()) {
            String value = valueIterator.next();
            XMLElement valueElement = createTextOnlyElement(NamespaceURIs.JABBER_X_DATA, "value", value);
            fieldElements.add(valueElement);
        }

        Iterator<Option> optionIterator = field.getOptions();
        while (optionIterator.hasNext()) {
            Option option = optionIterator.next();

            Attribute[] attributes = option.getLabel() == null ? null : new Attribute[] { new Attribute("label", option
                    .getLabel()) };
            XMLFragment[] elements = option.getValue() == null ? null : new XMLFragment[] { new XMLText(option
                    .getValue()) };

            XMLElement optionElement = new XMLElement(NamespaceURIs.JABBER_X_DATA, "option", null, attributes, elements);
            fieldElements.add(optionElement);
        }

        return new XMLElement(NamespaceURIs.JABBER_X_DATA, "field", null, fieldAttributes, fieldElements);

    }

    protected XMLElement createEmptyElement(String namespaceURI, String elementName) {
        return new XMLElement(namespaceURI, elementName, null, (Attribute[]) null, (XMLFragment[]) null);
    }

    protected XMLElement createTextOnlyElement(String namespaceURI, String elementName, String text) {
        return new XMLElement(namespaceURI, elementName, null, null, new XMLFragment[] { new XMLText(text) });
    }
}
