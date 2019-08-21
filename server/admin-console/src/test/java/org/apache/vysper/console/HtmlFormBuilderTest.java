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

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.junit.Assert;
import org.junit.Test;


public class HtmlFormBuilderTest {
    
    private static final String LABEL = "Some label";
    private static final String VALUE1 = "Value 1";
    private static final String VALUE2 = "Value 2";
    private static final String VALUE3 = "Value 3";

    private DataForm form = new DataForm(DataForm.Type.form);
    private HtmlFormBuilder builder = new HtmlFormBuilder();

    @Test
    public void testHiddenField() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.hidden);
        field.addValue(VALUE1);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><input id='abc-def' name='abc def' value='Value 1' type='hidden' /></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTextPrivateField() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.text_private);
        field.addValue(VALUE1);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><input id='abc-def' name='abc def' value='Value 1' type='password' /></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTextSingleField() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.text_single);
        field.addValue(VALUE1);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><input id='abc-def' name='abc def' value='Value 1' /></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFixedField() {
        FormField field = new FormField();
        field.addValue(VALUE1);
        field.setLabel(LABEL);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p>Some label <span>Value 1</span></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testJidSingleField() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.jid_single);
        field.addValue(VALUE1);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><input id='abc-def' name='abc def' value='Value 1' type='email' placeholder='example@vysper.org' /></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTextMultiField() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.text_multi);
        field.addValue(VALUE1);
        field.addValue(VALUE2);
        field.addValue(VALUE3);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><textarea id='abc-def' name='abc def'>" + VALUE1 + "\r\n" + VALUE2 + "\r\n" + VALUE3 + "</textarea></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testJidMultiField() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.jid_multi);
        field.addValue(VALUE1);
        field.addValue(VALUE2);
        field.addValue(VALUE3);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><textarea id='abc-def' name='abc def'>" + VALUE1 + "\r\n" + VALUE2 + "\r\n" + VALUE3 + "</textarea></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void listSingleFieldNoValue() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.list_single);
        field.addOption(new FormField.Option("Label 1", VALUE1));
        field.addOption(new FormField.Option(VALUE2));
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><select id='abc-def' name='abc def'><option value='" + VALUE1 + "'>Label 1</option><option value='" + VALUE2 + "'>" + VALUE2 + "</option></select></p>";
        
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void listSingleFieldWithValue() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.list_single);
        field.addOption(new FormField.Option("Label 1", VALUE1));
        field.addOption(new FormField.Option(VALUE2));
        field.addValue(VALUE1);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><select id='abc-def' name='abc def'><option value='" + VALUE1 + "' selected>Label 1</option><option value='" + VALUE2 + "'>" + VALUE2 + "</option></select></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void listMultiFieldNoValue() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.list_multi);
        field.addOption(new FormField.Option("Label 1", VALUE1));
        field.addOption(new FormField.Option(VALUE2));
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><select id='abc-def' name='abc def' multiple><option value='" + VALUE1 + "'>Label 1</option><option value='" + VALUE2 + "'>" + VALUE2 + "</option></select></p>";
        
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void listMultiFieldWithValue() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.list_multi);
        field.addOption(new FormField.Option("Label 1", VALUE1));
        field.addOption(new FormField.Option(VALUE2));
        field.addOption(new FormField.Option(VALUE3));
        field.addValue(VALUE1);
        field.addValue(VALUE3);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><select id='abc-def' name='abc def' multiple>" +
        		"<option value='" + VALUE1 + "' selected>Label 1</option>" +
        		"<option value='" + VALUE2 + "'>" + VALUE2 + "</option>" +
        		"<option value='" + VALUE3 + "' selected>" + VALUE3 + "</option>" +
        		"</select></p>";
        
        Assert.assertEquals(expected, actual);
    }
    
    
    @Test
    public void testBooleanFieldDefault() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.bool);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><input name='abc def' value='true' type='radio' /> Yes <input name='abc def' value='false' type='radio' selected /> No</p>"; 
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBooleanFieldWithValue() {
        FormField field = new FormField("abc def");
        field.setType(FormField.Type.bool);
        field.addValue("true");
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><input name='abc def' value='true' type='radio' selected /> Yes <input name='abc def' value='false' type='radio' /> No</p>"; 
        
        Assert.assertEquals(expected, actual);
    }

    
    @Test
    public void testNoTypeField() {
        FormField field = new FormField("abc def");
        field.addValue(VALUE1);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><input id='abc-def' name='abc def' value='Value 1' /></p>";
        
        Assert.assertEquals(expected, actual);
    }

    
    @Test
    public void testLabel() {
        FormField field = new FormField("abc def");
        field.setLabel(LABEL);
        field.addValue(VALUE1);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><label for='abc-def'>" + LABEL + "</label><input id='abc-def' name='abc def' value='Value 1' /></p>";
        
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDesc() {
        FormField field = new FormField("abc def");
        field.setDescription("Some description");
        field.addValue(VALUE1);
        form.addField(field);
        
        String actual = builder.build(form);
        String expected = "<p><input id='abc-def' name='abc def' value='Value 1' /><img src='resources/info.png' title='Some description' /></p>";
        
        Assert.assertEquals(expected, actual);
    }

}
