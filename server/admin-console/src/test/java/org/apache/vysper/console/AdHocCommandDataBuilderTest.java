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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.junit.Assert;
import org.junit.Test;


public class AdHocCommandDataBuilderTest {

    @Test
    public void multiRowValueWithCRLFMustGeneratedMultipleValues() {
        multiRowValueMustGeneratedMultipleValues(new String[]{"value 1\r\nvalue 2"});
    }

    @Test
    public void multiRowValueWithCRMustGeneratedMultipleValues() {
        multiRowValueMustGeneratedMultipleValues(new String[]{"value 1\rvalue 2"});
    }
    
    @Test
    public void multiRowValueWithLFMustGeneratedMultipleValues() {
        multiRowValueMustGeneratedMultipleValues(new String[]{"value 1\nvalue 2"});
    }
    
    private void multiRowValueMustGeneratedMultipleValues(String[] value) {
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        
        parameters.put(AdminConsoleController.SESSION_FIELD, new String[]{"sessionid"});
        parameters.put("test", value);
        
        AdHocCommandDataBuilder builder = new AdHocCommandDataBuilder();
        AdHocCommandData commandData = builder.build(parameters);
        
        DataForm form = commandData.getForm();
        Assert.assertFalse(form.getFields().isEmpty());
        FormField field = form.getFields().get(0);
        
        Iterator<CharSequence> values = field.getValues().iterator();
        Assert.assertEquals("value 1", values.next());
        Assert.assertEquals("value 2", values.next());
        Assert.assertFalse(values.hasNext());
    }

}
