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

import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 * provides utility methods for {@link org.apache.vysper.xmpp.addressing.Entity}
 */
public class EntityUtils {

    /**
     * checks a JID to be addressed to the server's domain
     * @param toVerify - JID to be verified 
     * @param serverJID - JID of the server
     * @return TRUE iff toVerify JID equals the server's JID
     */
    public static boolean isAddressingServer(Entity toVerify, Entity serverJID) {
        return toVerify.getDomain().toLowerCase().equals(serverJID.getDomain().toLowerCase());
    }
    
    /**
     * checks a JID to be addressed to any of the server's component
     * @param toVerify - JID to be verified 
     * @param serverJID - JID of the server
     * @return TRUE iff toVerify's domain is a component of the given server JID
     */
    public static boolean isAddressingServerComponent(Entity toVerify, Entity serverJID) {
        return toVerify.getDomain().endsWith("." + serverJID.getDomain());
    }

    /**
     * creates a JID with only a domain part set, the domain part being assembled from a subdomain and the server domain.
     * @param subdomain
     * @param serverRuntimeContext to retrieve the server JID
     * @return subdomain.serverdomain.tld
     */
    public static Entity createComponentDomain(String subdomain, ServerRuntimeContext serverRuntimeContext) {
        try {
            return EntityImpl.parse(subdomain + "." + serverRuntimeContext.getServerEntity().getDomain());
        } catch (EntityFormatException e) {
            // only happens when server entity is bad.
            throw new RuntimeException("could not create component domain", e);
        }
    }
    
    public static Entity createComponentDomain(String subdomain, Entity serverEntity){
        try {
            return EntityImpl.parse(subdomain + "." + serverEntity.getDomain());
        } catch (EntityFormatException e) {
            // only happens when server entity is bad.
            throw new RuntimeException("could not create component domain", e);
        }
    }
}
