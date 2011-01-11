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

import java.util.Iterator;

import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.packet.DataForm;

public class HtmlFormBuilder {

    public String build(DataForm form) {
        if(form == null) return "";
        
        StringBuffer sb = new StringBuffer();
        Iterator<String> instructions = form.getInstructions();
        while(instructions.hasNext()) {
            sb.append("<p class='instruction'>" + instructions.next() + "</p>");
        }
        
        Iterator<FormField> fields = form.getFields();
        while(fields.hasNext()) {
            FormField field = fields.next();
            String type = field.getType();
            System.out.println(type);
            sb.append("<p>");
            if("hidden".equals(type)) {
                sb.append(hiddenFieldToHtml(field));
            } else if("fixed".equals(type)) {
                sb.append(fixedFieldToHtml(field));
            } else if("jid-single".equals(type)) {
                sb.append(jidSingleFieldToHtml(field));
            } else if("text-single".equals(type)) {
                sb.append(textSingleFieldToHtml(field));
            } else if("text-private".equals(type)) {
                sb.append(textPrivateFieldToHtml(field));
            } else {
                System.out.println("Unknown field type: " + type);
            }
            sb.append("</p>");
        }
        
        return sb.toString();
    }
    
    private String labelToHtml(FormField field) {
        StringBuffer sb = new StringBuffer();
        if(field.getLabel() != null) {
            sb.append("<label>");
            // TODO for
            sb.append(field.getLabel());
            sb.append("</label>");
        }
        return sb.toString();
    }
    
    private String hiddenFieldToHtml(FormField field) {
        return labelToHtml(field) + "<input type='hidden' name='" + field.getVariable() + "' value='" + getSingleValue(field) + "' />"; 
    }

    private String fixedFieldToHtml(FormField field) {
        return labelToHtml(field) + " <span>" + field.getValues().next() + "</span>"; 
    }

    private String jidSingleFieldToHtml(FormField field) {
        return labelToHtml(field) + "<input name='" + field.getVariable() + "' value='" + getSingleValue(field) + "' />";
    }
    
    private String textSingleFieldToHtml(FormField field) {
        return labelToHtml(field) + "<input name='" + field.getVariable() + "' value='" + getSingleValue(field) + "' />";
    }

    private String textPrivateFieldToHtml(FormField field) {
        return labelToHtml(field) + "<input name='" + field.getVariable() + "' type='password' value='" + getSingleValue(field) + "' />";
    }
    
    private String getSingleValue(FormField field) {
        if(field.getValues().hasNext()) {
            return field.getValues().next();
        } else {
            return "";
        }
    }

}
