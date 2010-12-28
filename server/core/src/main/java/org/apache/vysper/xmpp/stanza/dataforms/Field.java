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

/**
 */
public class Field {

    public static enum Type {

        BOOLEAN("boolean"), FIXED("fixed"), HIDDEN("hidden"), JID_MULTI("jid-multi"), JID_SINGLE("jid-single"), LIST_MULTI(
                "list-multi"), LIST_SINGLE("list-single"), TEXT_MULTI("text-multi"), TEXT_PRIVATE("text-private"), TEXT_SINGLE(
                "text-single");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static boolean isMulti(Type type) {
            return type != null && (type == JID_MULTI || type == LIST_MULTI || type == TEXT_MULTI);   
        }
    }

    public static final String FORM_TYPE = "FORM_TYPE";

    protected String label;

    protected Type type;

    protected String var;

    protected String desc;

    protected boolean required = false;

    protected final List<Option> options = new ArrayList<Option>();

    protected final List<String> values = new ArrayList<String>();

    public Field(String label, Type type, String var) {
        this.label = label;
        this.type = type;
        this.var = var;
    }

    /**
     * Create field with a single value
     * @param label The value of the  "label" attribute
     * @param type The value of the "type" attribute
     * @param var The value of the "var" attribute
     * @param value The text of the initial "value" element
     */
    public Field(String label, Type type, String var, String value) {
        this.label = label;
        this.type = type;
        this.var = var;
        addValue(value);
    }
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getVar() {
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Iterator<String> getValueIterator() {
        return values.iterator();
    }

    public void addValue(String value) {
        this.values.add(value);
    }

    public Iterator<Option> getOptions() {
        return options.iterator();
    }

    public void addOption(Option option) {
        this.options.add(option);
    }
}
