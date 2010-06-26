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

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * This visitor visits all member-affiliations of a given node and collects it for later retrieval.
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class CollectingMemberAffiliationVisitor implements MemberAffiliationVisitor {
    // the list of user <-> nodeName affiliation
    protected List<AffiliationItem> affiliations = null;

    private String nodeName;

    /**
     * Create a new visitor preconfigured with the node name.
     * @param nodeName
     */
    public CollectingMemberAffiliationVisitor(String nodeName) {
        this.nodeName = nodeName;
        this.affiliations = new ArrayList<AffiliationItem>();
    }

    /**
     * Returns the collected affiliations.
     */
    public List<AffiliationItem> getAffiliations() {
        return affiliations;
    }

    public void visit(Entity jid, PubSubAffiliation affiliation) {
        affiliations.add(new AffiliationItem(this.nodeName, jid, affiliation));
    }
}
