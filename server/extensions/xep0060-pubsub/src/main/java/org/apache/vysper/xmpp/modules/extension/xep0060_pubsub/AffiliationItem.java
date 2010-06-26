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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class AffiliationItem {

    protected String nodeName = null;

    protected Entity jid = null;

    protected PubSubAffiliation affiliation = null;

    /**
     * Creates a new affiliation item with the supplied values.
     */
    public AffiliationItem(String nodeName, Entity jid, PubSubAffiliation affil) {
        this.nodeName = nodeName;
        this.jid = jid;
        this.affiliation = affil;
    }

    /**
     * Returns the node of this affiliation.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * Returns the state of the affiliation.
     */
    public PubSubAffiliation getAffiliation() {
        return affiliation;
    }

    /**
     * Returns the JID of the affiliation.
     */
    public Entity getJID() {
        return jid;
    }
}
