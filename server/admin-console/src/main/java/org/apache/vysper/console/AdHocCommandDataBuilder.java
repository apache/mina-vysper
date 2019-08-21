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

import java.util.Map;
import java.util.Map.Entry;

import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
import org.jivesoftware.smackx.xdata.FormField;

/**
 * Builds {@link AdHocCommandData} from posted data
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class AdHocCommandDataBuilder {

    /**
     * Builds {@link AdHocCommandData} from posted data
     * @param parameters
     * @return
     */
    public AdHocCommandData build(Map<String, String[]> parameters) {
        AdHocCommandData commandData = new AdHocCommandData();
        commandData.setSessionID(getSingleValue(parameters, AdminConsoleController.SESSION_FIELD));
        
        DataForm form = new DataForm(DataForm.Type.submit);
        
        for(Entry<String, String[]> entry : parameters.entrySet()) {
            if(!AdminConsoleController.SESSION_FIELD.equals(entry.getKey())) {
                FormField field = new FormField(entry.getKey());
                for(String value : entry.getValue()) {
                    String[] splitValues = value.split("[\\r\\n]+");
                    for(String splitValue : splitValues) {
                        field.addValue(splitValue);
                    }
                }
                form.addField(field);
            }
        }
        
        commandData.setForm(form);
        
        return commandData;
    }
    
    private String getSingleValue(Map<String, String[]> parameters, String name) {
        String[] values = parameters.get(name);
        if(values != null && values.length == 1) {
            return values[0];
        } else {
            throw new IllegalArgumentException(name + " must contain exactly one value");
        }
    }

}
