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
public class Feature implements InfoElement {

    private static final Integer CLASS_ID = 2;

    protected String var;

    public Feature(String var) {
        if (StringUtils.isEmpty(var))
            throw new IllegalArgumentException("var may not be null");
        this.var = var;
    }

    public String getVar() {
        return var;
    }

    public Integer getElementClassId() {
        return CLASS_ID;
    }

    public void insertElement(StanzaBuilder stanzaBuilder) {
        stanzaBuilder.startInnerElement("feature", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO).addAttribute("var",
                var).endInnerElement();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + var.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        Feature other = (Feature) obj;
        if (!var.equals(other.var)) return false;
        
        return true;
    }
    
    
}
