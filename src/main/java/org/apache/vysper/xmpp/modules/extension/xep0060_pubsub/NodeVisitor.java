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
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;

/**
 * The NodeVisitor is used to visit all subscriptions of a collection node.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec="xep-0060", status= SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.UNSUPPORTED)
public interface NodeVisitor {

    /**
     * visit is part of the "visitor pattern" and should be used to iterate
     * over all nodes of a collection node.
     * 
     * @param ln the node (for more detailed data)
     */
    void visit(LeafNode ln);

    /**
     * @return the ordered list of items.
     */
    List<Item> getNodeItemList();

}
