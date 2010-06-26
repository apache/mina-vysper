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

/**
 * This class defines which affiliations are known. The order of the
 * constants is important and defines the hierarchy.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public enum PubSubAffiliation {
    OUTCAST("outcast"), NONE("none"), MEMBER("member"), PUBLISHER("publisher"), OWNER("owner");

    private final String xep0060Name;

    private PubSubAffiliation(String name) {
        this.xep0060Name = name;
    }

    public String toString() {
        return xep0060Name;
    }

    /**
     * Returns the correct PubSubAffiliation object for the given string.
     * The case will be ignored.
     *
     * @param name The name of the requested affiliation object.
     * @return the affiliation object, NONE if not known.
     */
    public static PubSubAffiliation get(String name) {
        if (name.equalsIgnoreCase(OUTCAST.toString()))
            return OUTCAST;
        if (name.equalsIgnoreCase(MEMBER.toString()))
            return MEMBER;
        if (name.equalsIgnoreCase(PUBLISHER.toString()))
            return PUBLISHER;
        if (name.equalsIgnoreCase(OWNER.toString()))
            return OWNER;
        return NONE;
    }
}
