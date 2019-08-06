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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.junit.Assert;
import org.junit.Test;

public class DataFormParserTestCase {

    private static final Entity E1 = EntityImpl.parseUnchecked("user1@vysper.org");
    private static final Entity E2 = EntityImpl.parseUnchecked("user2@vysper.org");
    
    @Test
    public void noType() {
        assertSingleValue(null, "val1", "val1");
    }
    
    @Test
    public void textSingle() {
        assertSingleValue("text-single", "val1", "val1");
    }
    
    @Test
    public void textPrivate() {
        assertSingleValue("text-private", "val1", "val1");
    }
    
    @Test
    public void hidden() {
        assertSingleValue("hidden", "val1", "val1");
    }
    
    @Test
    public void fixed() {
        assertSingleValue("fixed", "val1", "val1");
    }
    
    @Test
    public void listSingle() {
        assertSingleValue("list-single", "val1", "val1");
    }
    
    @Test
    public void jidSingle() {
        assertSingleValue("jid-single", E1.getFullQualifiedName(), E1);
    }
    
    @Test
    public void booleanFalse() {
        assertSingleValue("boolean", "false", Boolean.FALSE);
    }
    
    @Test
    public void booleanTrue() {
        assertSingleValue("boolean", "true", Boolean.TRUE);
    }
    
    @Test
    public void boolean1() {
        assertSingleValue("boolean", "1", Boolean.TRUE);
    }
    
    @Test
    public void boolean0() {
        assertSingleValue("boolean", "0", Boolean.FALSE);
    }
    
    @Test
    public void textMulti() {
        assertMultiValue("text-multi", Arrays.asList("val1", "val2"), Arrays.asList((Object)"val1", "val2"));
    }
    
    @Test
    public void listMulti() {
        assertMultiValue("list-multi", Arrays.asList("val1", "val2"), Arrays.asList((Object)"val1", "val2"));
    }
    
    @Test
    public void jidMulti() {
        assertMultiValue("jid-multi", Arrays.asList(E1.getFullQualifiedName(), E2.getFullQualifiedName()), Arrays.asList((Object)E1, E2));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void invalidFieldType() {
        assertSingleValue("dummy", null, null);
    }
    
    private void assertSingleValue(String type, String value, Object expectedValue) {
        XMLElementBuilder builder = new XMLElementBuilder("x", NamespaceURIs.JABBER_X_DATA)
            .startInnerElement("field", NamespaceURIs.JABBER_X_DATA)
            .addAttribute("var", "fie1");
        
        if(type != null) builder.addAttribute("type", type);
        
        builder.startInnerElement("value", NamespaceURIs.JABBER_X_DATA)
            .addText(value);
        
        XMLElement elm = builder.build(); 
        
        DataFormParser parser = new DataFormParser(elm);
        
        Map<String, Object> values = parser.extractFieldValues();
        
        Assert.assertEquals(1, values.size());
        Assert.assertEquals(expectedValue, values.get("fie1"));
        
    }

    private void assertMultiValue(String type, List<String> values, List<Object> expectedValues) {
        XMLElementBuilder builder = new XMLElementBuilder("x", NamespaceURIs.JABBER_X_DATA)
        .startInnerElement("field")
        .addAttribute("var", "fie1");
        
        if(type != null) builder.addAttribute("type", type);
        
        
        for(String value : values) {
            builder.startInnerElement("value");
            builder.addText(value);
            builder.endInnerElement();
        }
        
        XMLElement elm = builder.build(); 
        
        DataFormParser parser = new DataFormParser(elm);
        
        Map<String, Object> actualValues = parser.extractFieldValues();
        
        Assert.assertEquals(1, actualValues.size());
        Assert.assertEquals(expectedValues, actualValues.get("fie1"));
    }
}
