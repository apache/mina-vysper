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
package org.apache.vysper.xmpp.delivery;

import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class LocalDeliveryUtils {

    final static Logger logger = LoggerFactory.getLogger(LocalDeliveryUtils.class);

    /**
     * delivers a stanza to a server-local resource. used for sending a stanza to all resources of the same user.
     * @param registry registry to look up session by resource ID
     * @param resource receiving resource ID
     * @param push stanza to be pushed
     */
    public static void relayToResourceDirectly(ResourceRegistry registry, String resource, Stanza push) {
        try {
            SessionContext targetContext = registry.getSessionContext(resource);
            if (targetContext == null)
                return;
            targetContext.getResponseWriter().write(push);
        } catch (RuntimeException e) {
            logger.warn("failed to directly relay stanza to resource " + resource, e);
        }
    }

}
