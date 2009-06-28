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

import java.util.List;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * The SubscriberVisitor is used to visit all subscriptions of a node.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public interface ItemVisitor {

    /**
     * visit is part of the "visitor pattern" and should be used to iterate
     * over all subscriptions of a node.
     * 
     * @param itemID the id of the message
     * @param payload the payload of the message
     */
    void visit(String itemID, XMLElement payload);
    
    /**
     * @return the ordered list of items.
     */
    public List<Item> getItemList();

}
