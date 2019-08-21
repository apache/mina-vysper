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

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

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
        List<String> instructions = form.getInstructions();
        for (String instruction: instructions) {
            sb.append("<p class='instruction'>" + instruction + "</p>");
        }

        Iterator<FormField> fields = form.getFields().iterator();
        while (fields.hasNext()) {
            FormField field = fields.next();
            FormField.Type type = field.getType();
            sb.append("<p>");
            if (type == FormField.Type.hidden) {
                sb.append(hiddenFieldToHtml(field));
            } else if (type == FormField.Type.fixed) {
                sb.append(fixedFieldToHtml(field));
            } else if (type == FormField.Type.jid_single) {
                sb.append(jidSingleFieldToHtml(field));
            } else if (type == FormField.Type.text_single || type == null) {
                sb.append(textSingleFieldToHtml(field));
            } else if (type == FormField.Type.text_private) {
                sb.append(textPrivateFieldToHtml(field));
            } else if (type == FormField.Type.text_multi) {
                sb.append(textMultiFieldToHtml(field));
            } else if (type == FormField.Type.list_single) {
                sb.append(listSingleFieldToHtml(field));
            } else if (type == FormField.Type.list_multi) {
                sb.append(listMultiFieldToHtml(field));
            } else if (type == FormField.Type.jid_multi) {
                // for now, do jid-multi as a textarea
                sb.append(textMultiFieldToHtml(field));
            } else if (type == FormField.Type.bool) {
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
        sb.append(" <span>" + field.getValues().get(0) + "</span>");
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
        Iterator<CharSequence> values = field.getValues().iterator();
        while(values.hasNext()) {
            if(!first) sb.append("\r\n");
            sb.append(values.next());
            first = false;
        }
        
        sb.append("</textarea>");
        return sb.toString();
    }
    
    private String listSingleFieldToHtml(FormField field) {
        Iterator<CharSequence> fieldValues = field.getValues().iterator();
        List<CharSequence> values = new ArrayList<>();
        if(fieldValues.hasNext()) values.add(fieldValues.next());
        
        return listFieldToHtml(field, values, false);
    }

    private String listMultiFieldToHtml(FormField field) {
        Iterator<CharSequence> fieldValues = field.getValues().iterator();
        List<CharSequence> values = new ArrayList<>();
        while(fieldValues.hasNext()) {
            values.add(fieldValues.next());
        }
        
        return listFieldToHtml(field, values, true);
    }

    private String listFieldToHtml(FormField field, List<CharSequence> values, boolean multiple) {
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
        
        Iterator<FormField.Option> options = field.getOptions().iterator();
        
        while(options.hasNext()) {
            FormField.Option option = options.next();
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
        boolean value = (!field.getValues().isEmpty() && "true".equals(field.getValues().get(0).toString()));
        
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

    private CharSequence getSingleValue(FormField field) {
        if (!field.getValues().isEmpty()) {
            return field.getValues().get(0);
        } else {
            return "";
        }
    }
    
    private String varToId(String var) {
        return var.replaceAll("[^A-Za-z0-9]", "-");
    }

}
