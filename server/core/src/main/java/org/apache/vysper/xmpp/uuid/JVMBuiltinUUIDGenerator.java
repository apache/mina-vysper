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
package org.apache.vysper.xmpp.uuid;

import java.util.UUID;

/**
 * utilize the JVM's UUID generator
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class JVMBuiltinUUIDGenerator implements UUIDGenerator {

    public String create() {
        // generates UUID-type 4, pseudo random session id
        // TODO is this random enough?
        String uuidRaw = UUID.randomUUID().toString();
        String uuidTrimmed = uuidRaw.replace("-", "");
        return uuidTrimmed;
    }

}
