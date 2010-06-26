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

/**
 * <code>
 * The "JID"<br/>
 * jid             = [ node "@" ] domain [ "/" resource ]<br/>
 * domain          = fqdn / address-literal<br/>
 * fqdn            = (sub-domain 1*("." sub-domain))<br/>
 * sub-domain      = (internationalized domain label)<br/>
 * address-literal = IPv4address / IPv6address<br/>
 *<br/>
 * Each allowable portion of a JID (node identifier, domain identifier,
 * and resource identifier) MUST NOT be more than 1023 bytes in length,
 * resulting in a maximum total size (including the '@' and '/'
 * separators) of 3071 bytes.
 * </code>
 * <br/><br/>
 * romeo@example.net - typical user/client JID<br/>
 * example.net - typical server JID<br/>
 * node@domain - a BARE JID<br/>
 * node@domain/resource - a FULL JID - client id after resource binding, a CONNECTED RESOURCE<br/>
 *
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 *
 */
//  TODO add unit tests for all implementors
public interface Entity {

    String getNode();

    String getDomain();

    String getResource();

    /**
     * @return string like "node@domain/resource"
     */
    String getFullQualifiedName();

    /**
     * @return string like "node@domain"
     */
    Entity getBareJID();

    String getCanonicalizedName();

    boolean isNodeSet();

    boolean isResourceSet();

}
