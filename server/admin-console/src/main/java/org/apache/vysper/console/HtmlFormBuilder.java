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
package org.apache.vysper.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.FormField.Option;
import org.jivesoftware.smackx.packet.DataForm;

/**
 * Builds an HTML form from a {@link DataForm}
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class HtmlFormBuilder {

    /**
     * Builds an HTML form from a {@link DataForm}
     * @param form
     * @return
     */
    public String build(DataForm form) {
        if (form == null)
            return "";

        StringBuilder sb = new StringBuilder();
        Iterator<String> instructions = form.getInstructions();
        while (instructions.hasNext()) {
            sb.append("<p class='instruction'>" + instructions.next() + "</p>");
        }

        Iterator<FormField> fields = form.getFields();
        while (fields.hasNext()) {
            FormField field = fields.next();
            String type = field.getType();
            sb.append("<p>");
            if ("hidden".equals(type)) {
                sb.append(hiddenFieldToHtml(field));
            } else if ("fixed".equals(type)) {
                sb.append(fixedFieldToHtml(field));
            } else if ("jid-single".equals(type)) {
                sb.append(jidSingleFieldToHtml(field));
            } else if ("text-single".equals(type) || type == null) {
                sb.append(textSingleFieldToHtml(field));
            } else if ("text-private".equals(type)) {
                sb.append(textPrivateFieldToHtml(field));
            } else if ("text-multi".equals(type)) {
                sb.append(textMultiFieldToHtml(field));
            } else if ("list-single".equals(type)) {
                sb.append(listSingleFieldToHtml(field));
            } else if ("list-multi".equals(type)) {
                sb.append(listMultiFieldToHtml(field));
            } else if ("jid-multi".equals(type)) {
                // for now, do jid-multi as a textarea
                sb.append(textMultiFieldToHtml(field));
            } else if ("boolean".equals(type)) {
                sb.append(booleanFieldToHtml(field));
            } else {
                throw new RuntimeException("Unknown field type: " + type);
            }
            sb.append(descToHtml(field));
            sb.append("</p>");
        }

        return sb.toString();
    }

    private String labelToHtml(FormField field) {
        StringBuilder sb = new StringBuilder();
        if (field.getLabel() != null) {
            sb.append("<label for='");
            sb.append(varToId(field.getVariable()));
            sb.append("'>");
            sb.append(field.getLabel());
            sb.append("</label>");
        }
        return sb.toString();
    }

    private String descToHtml(FormField field) {
        StringBuilder sb = new StringBuilder();
        if (field.getDescription() != null) {
            sb.append("<img src='resources/info.png' title='");
            sb.append(field.getDescription());
            sb.append("' />");
        }
        return sb.toString();
    }
    
    private String fixedFieldToHtml(FormField field) {
        StringBuilder sb = new StringBuilder();
        if (field.getLabel() != null)
            sb.append(field.getLabel());
        sb.append(" <span>" + field.getValues().next() + "</span>");
        return sb.toString();
    }

    private String hiddenFieldToHtml(FormField field) {
        return singleValueFieldToHtml(field, "hidden", null);
    }
    
    private String jidSingleFieldToHtml(FormField field) {
        return singleValueFieldToHtml(field, "email", "example@vysper.org");
    }

    private String textSingleFieldToHtml(FormField field) {
        return singleValueFieldToHtml(field, null, null);
    }

    private String textPrivateFieldToHtml(FormField field) {
        return singleValueFieldToHtml(field, "password", null);
    }
    
    private String textMultiFieldToHtml(FormField field) {
        StringBuilder sb = new StringBuilder();
        sb.append(labelToHtml(field));
        sb.append("<textarea id='");
        sb.append(varToId(field.getVariable()));
        sb.append("' name='");
        sb.append(field.getVariable());
        sb.append("'>");
        
        boolean first = true;
        Iterator<String> values = field.getValues();
        while(values.hasNext()) {
            if(!first) sb.append("\r\n");
            sb.append(values.next());
            first = false;
        }
        
        sb.append("</textarea>");
        return sb.toString();
    }
    
    private String listSingleFieldToHtml(FormField field) {
        Iterator<String> fieldValues = field.getValues();
        List<String> values = new ArrayList<String>();
        if(fieldValues.hasNext()) values.add(fieldValues.next());
        
        return listFieldToHtml(field, values, false);
    }

    private String listMultiFieldToHtml(FormField field) {
        Iterator<String> fieldValues = field.getValues();
        List<String> values = new ArrayList<String>();
        while(fieldValues.hasNext()) {
            values.add(fieldValues.next());
        }
        
        return listFieldToHtml(field, values, true);
    }

    private String listFieldToHtml(FormField field, List<String> values, boolean multiple) {
        StringBuilder sb = new StringBuilder();
        sb.append(labelToHtml(field));
        sb.append("<select id='");
        sb.append(varToId(field.getVariable()));
        sb.append("' name='");
        sb.append(field.getVariable());
        sb.append("'");
        if(multiple) {
            sb.append(" multiple");
        }
        sb.append(">");
        
        Iterator<Option> options = field.getOptions();
        
        while(options.hasNext()) {
            Option option = options.next();
            sb.append("<option value='");
            sb.append(option.getValue());
            sb.append("'");
            
            if(values.contains(option.getValue())) {
                sb.append(" selected");
            }
            sb.append(">");
            if(option.getLabel() != null) {
                sb.append(option.getLabel());
            } else {
                sb.append(option.getValue());
            }
        
            sb.append("</option>");
        }
        
        
        
        sb.append("</select>");
        return sb.toString();
    }


    
    private String booleanFieldToHtml(FormField field) {
        StringBuilder sb = new StringBuilder();
        boolean value = (field.getValues().hasNext() && "true".equals(field.getValues().next()));
        
        sb.append(labelToHtml(field));
        sb.append("<input name='");
        sb.append(field.getVariable());
        sb.append("' value='true' type='radio' ");
        if(value) sb.append("selected ");
        sb.append("/> Yes ");
        sb.append("<input name='");
        sb.append(field.getVariable());
        sb.append("' value='false' type='radio' ");
        if(!value) sb.append("selected ");
        sb.append("/> No");

        
        return sb.toString();
    }

    private String singleValueFieldToHtml(FormField field, String type, String placeholder) {
        StringBuilder sb = new StringBuilder();
        sb.append(labelToHtml(field));
        sb.append("<input id='");
        sb.append(varToId(field.getVariable()));
        sb.append("' name='");
        sb.append(field.getVariable());
        sb.append("' value='");
        sb.append(getSingleValue(field));
        sb.append("'");
        if(type != null) {
            sb.append(" type='" + type + "'");
        }
        if(placeholder != null) {
            sb.append(" placeholder='" + placeholder + "'");
        }
        sb.append(required(field));
        sb.append(" />");
        return sb.toString();
    }

    private String required(FormField field) {
        return field.isRequired() ? "required" : "";
    }

    private String getSingleValue(FormField field) {
        if (field.getValues().hasNext()) {
            return field.getValues().next();
        } else {
            return "";
        }
    }
    
    private String varToId(String var) {
        return var.replaceAll("[^A-Za-z0-9]", "-");
    }

}
