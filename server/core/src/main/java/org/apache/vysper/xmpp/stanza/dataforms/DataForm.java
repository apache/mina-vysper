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

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage;
import org.apache.vysper.compliance.SpecCompliant.ComplianceStatus;

/**
 * object model for data forms, according to XEP-0004, as used by many XMPP extensions (and the core).
 * 
 * to encode this model, use {@link org.apache.vysper.xmpp.stanza.dataforms.DataFormEncoder} 
 */
@SpecCompliant(spec = "XEP-0004", status = ComplianceStatus.IN_PROGRESS, coverage = ComplianceCoverage.PARTIAL)
public class DataForm {

    public static enum Type {

        cancel, form, result, submit;

        public String value() {
            return name();
        }

    }

    protected Type type;

    protected List<String> instructions = new ArrayList<String>();

    protected String title;

    protected final List<Field> fields = new ArrayList<Field>();

    protected final List<List<Field>> items = new ArrayList<List<Field>>();

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Iterator<String> getInstructionIterator() {
        return instructions.iterator();
    }

    public void addInstruction(String instructions) {
        this.instructions.add(instructions);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Iterator<Field> getFieldIterator() {
        return fields.iterator();
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public Iterator<List<Field>> getItemIterator() {
        return items.iterator();
    }

    public void addItem(List<Field> item) {
        items.add(item);
    }

    public Iterator<Field> getReportedIterator() {
        List<Field> reportedFields = new ArrayList<Field>();

        if (items.size() == 0)
            return reportedFields.iterator();

        // reported fields are implicitly defined by item fields
        // here, reported fields are only taken from the first item.
        // there is no consistency check if the remaining fields report the same fields,
        // as the spec requires (XEP-004#3.4, last sentence).
        List<Field> fieldPrototype = items.get(0);
        for (Field field : fieldPrototype) {
            // copy the relevant reported information
            reportedFields.add(new Field(field.getLabel(), field.getType(), field.getVar()));
        }

        return reportedFields.iterator();
    }

}
