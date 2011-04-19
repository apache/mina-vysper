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
package org.apache.vysper.xmpp.addressing.stringprep;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.compliance.SpecCompliant.ComplianceStatus;

/**
 * This class is used to prepare a Node Identifier for further usage.
 * 
 * see RFC3920:3.3
 * see RFC3920:Appendix A
 * see http://www.ietf.org/rfc/rfc3920.txt
 * @author Gerolf Seitz (gseitz@apache.org)
 * 
 */
@SpecCompliant(spec = "RFC3920", section = "A", status = ComplianceStatus.IN_PROGRESS)
public class NodePrep extends StringPrep {

    private static final NodePrep INSTANCE = new NodePrep();

    /**
     * Applies the Nodeprep profile to the given node.
     * 
     * @param node
     *            the node to prepare
     * @return the prepared node
     * @throws StringPrepViolationException
     *             in case the Nodeprep profile can't be applied
     */
    public static String prepare(String node) throws StringPrepViolationException {
        return INSTANCE.prepareString(node);
    }

    private NodePrep() {
    }

    @Override
    @SpecCompliant(spec = "RFC3920", section = "A.3", status = ComplianceStatus.FINISHED)
    protected Map<String, String> buildMapping() {
        Map<String, String> mapping = new HashMap<String, String>();
        mapping.putAll(StringPrepConstants.B_1_CommonlyMappedtoNothing);
        mapping.putAll(StringPrepConstants.B_2_MappingForCaseFoldingUsedWithKFC);

        return mapping;
    }

    @Override
    @SpecCompliant(spec = "RFC3920", section = "A.5", status = ComplianceStatus.FINISHED)
    protected Set<String> buildProhibitedSet() {
        Set<String> prohibited = super.buildProhibitedSet();

        prohibited.add("\\u0022");
        prohibited.add("\u0026");
        prohibited.add("\u0027");
        prohibited.add("\u002F");
        prohibited.add("\u003A");
        prohibited.add("\u003C");
        prohibited.add("\u003E");
        prohibited.add("\u0040");

        return prohibited;
    }
}
