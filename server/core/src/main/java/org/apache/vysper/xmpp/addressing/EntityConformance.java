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

package org.apache.vysper.xmpp.addressing;

import org.apache.vysper.compliance.SpecCompliant;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "RFC3920", section = "3.1")
public class EntityConformance {

    public static boolean checkRFC3920Conformance(Entity entity) {
        if (!checkPartConformity(entity.getDomain()))
            return false;
        if (!checkPartConformity(entity.getNode()))
            return false;
        if (!checkPartConformity(entity.getResource()))
            return false;

        if (checkPartIsEmpty(entity.getDomain()))
            return false;
        return true;
    }

    private static boolean checkPartConformity(String part) {
        return part == null || part.getBytes().length <= 1023;
    }

    private static boolean checkPartIsEmpty(String part) {
        return part == null || "".equals(part);
    }

}
