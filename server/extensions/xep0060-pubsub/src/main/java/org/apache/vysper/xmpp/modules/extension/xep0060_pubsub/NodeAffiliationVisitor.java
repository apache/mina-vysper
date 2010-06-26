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
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;

/**
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class NodeAffiliationVisitor implements NodeVisitor {

    // bare jid to compare with
    protected Entity bareJID = null;

    // the list of user <-> node affiliation
    protected List<AffiliationItem> affiliations = null;

    /**
     * Creates a new node visitor to fetch all affiliations for the given user.
     */
    public NodeAffiliationVisitor(Entity entity) {
        this.bareJID = entity.getBareJID();
        this.affiliations = new ArrayList<AffiliationItem>();
    }

    /**
     * Visit all nodes and get the affiliation of the requesting user.
     */
    public void visit(LeafNode ln) {
        PubSubAffiliation affil = ln.getAffiliation(bareJID);
        if (!affil.equals(PubSubAffiliation.NONE)) {
            AffiliationItem ai = new AffiliationItem(ln.getName(), bareJID, affil);
            affiliations.add(ai);
        }
    }

    /**
     * Return the list of known affiliations (other than "NONE").
     */
    public List<AffiliationItem> getAffiliations() {
        return affiliations;
    }

}
