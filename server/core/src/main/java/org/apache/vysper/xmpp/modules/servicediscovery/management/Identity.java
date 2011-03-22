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
package org.apache.vysper.xmpp.modules.servicediscovery.management;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Identity implements InfoElement {

    private static final Integer CLASS_ID = 1;

    protected String category; // required

    protected String type; // required

    protected String name; // optional

    public Identity(String category, String type, String name) {
        if (StringUtils.isEmpty(category))
            throw new IllegalArgumentException("category may not be null");
        if (StringUtils.isEmpty(type))
            throw new IllegalArgumentException("type may not be null");
        this.category = category;
        this.type = type;
        this.name = name;
    }

    public Identity(String category, String type) {
        this(category, type, null);
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Integer getElementClassId() {
        return CLASS_ID;
    }

    public void insertElement(StanzaBuilder stanzaBuilder) {
        stanzaBuilder.startInnerElement("identity", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO).addAttribute(
                "category", category).addAttribute("type", type);
        if (name != null) {
            stanzaBuilder.addAttribute("name", name);
        }
        stanzaBuilder.endInnerElement();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        Identity other = (Identity) obj;
        if (!category.equals(other.category)) return false;
        
        if (!name.equals(other.name)) return false;

        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type)) return false;
        
        return true;
    }
    
    
}
