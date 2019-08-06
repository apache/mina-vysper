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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class DataFormParser {

    private final Logger logger = LoggerFactory.getLogger(DataFormParser.class);

    public static Object extractFieldValue(String valueAsString, final Field.Type fieldType) throws IllegalArgumentException {
        Object value = null;
        if (fieldType == null) return valueAsString;

        switch (fieldType) {
            case LIST_MULTI:
            case LIST_SINGLE:
            case TEXT_MULTI:
            case TEXT_PRIVATE:
            case TEXT_SINGLE:
            case HIDDEN:
            case FIXED:
                value = valueAsString;
                break;
            case BOOLEAN:
                value = "1".equals(valueAsString) || "true".equals(valueAsString);
                break;
            case JID_MULTI:
            case JID_SINGLE:
                try {
                    value = EntityImpl.parse(valueAsString);
                } catch (EntityFormatException e) {
                    throw new IllegalArgumentException(e);
                }
                break;
        }
        return value;
    }

    protected XMLElement form;

    public DataFormParser(XMLElement form) {
        this.form = form;
    }

    public Map<String, Object> extractFieldValues() throws IllegalArgumentException {
        Map<String,Object> map = new LinkedHashMap<String, Object>();

        for (XMLElement fields : form.getInnerElementsNamed("field")) {
            final String varName = fields.getAttributeValue("var");
            final String typeName = fields.getAttributeValue("type");
            String valueAsString = null;

            // default to TEXT_SINGLE
            Field.Type fieldType = Field.Type.TEXT_SINGLE;
            if(typeName != null) {
                try {
                    fieldType = Field.Type.valueOf(typeName.toUpperCase().replace('-', '_'));
                } catch (IllegalArgumentException e) {
                    throw e;
                }
            }
            boolean isMulti = Field.Type.isMulti(fieldType);

            List<Object> values = isMulti ? new ArrayList<Object>() : null;
            for (XMLElement valueCandidates : fields.getInnerElementsNamed("value")) {
                final XMLText firstInnerText = valueCandidates.getFirstInnerText();
                if (firstInnerText != null) valueAsString = firstInnerText.getText();
                Object value;
                try {
                    value = extractFieldValue(valueAsString, fieldType);
                } catch (IllegalArgumentException e) {
                    logger.warn("malformed field value for field = " + varName + " and raw value = " + valueAsString);
                    continue;
                }
                if (!isMulti) {
                    map.put(varName, value);
                    break;
                } else {
                    values.add(value);
                }
            }
            if (isMulti) map.put(varName, values);
        }

        return map;
    }
}
