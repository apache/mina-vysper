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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Item {

    protected Entity jid; // required

    protected String name; // optional

    protected String node; // optional

    public Item(Entity jid, String name, String node) {
        if (jid == null)
            throw new IllegalArgumentException("jid may not be null");
        this.jid = jid;
        this.name = name;
        this.node = node;
    }

    public Item(Entity jid, String name) {
        this(jid, name, null);
    }

    public Item(Entity jid) {
        this(jid, null, null);
    }

    public Entity getJid() {
        return jid;
    }

    public String getName() {
        return name;
    }

    public String getNode() {
        return node;
    }

    public void insertElement(StanzaBuilder stanzaBuilder) {
        stanzaBuilder.startInnerElement("item", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS);
        if (jid != null)
            stanzaBuilder.addAttribute("jid", jid.getFullQualifiedName());
        if (name != null)
            stanzaBuilder.addAttribute("name", name);
        if (node != null)
            stanzaBuilder.addAttribute("node", node);
        stanzaBuilder.endInnerElement();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + jid.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        Item other = (Item) obj;
        if (!jid.equals(other.jid)) return false;
        
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name)) return false;
        
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node)) return false;
        
        return true;
    }
    
    
}
