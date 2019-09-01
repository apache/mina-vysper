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
package org.apache.vysper.xmpp.state.resourcebinding;

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.server.StanzaReceivingSessionContext;

/**
 * Resource registry giving access to its internal parts. This abstraction
 * should only be provided to internal framework parts. Technical components
 * like {@link org.apache.vysper.xmpp.protocol.StanzaHandler},
 * {@link org.apache.vysper.xmpp.modules.Module} should not be provided the
 * methods described by this contract.
 * 
 * @author Réda Housni Alaoui
 */
public interface InternalResourceRegistry extends ResourceRegistry {

    StanzaReceivingSessionContext getSessionContext(String resourceId);

    List<StanzaReceivingSessionContext> getSessions(Entity entity);

    List<StanzaReceivingSessionContext> getSessions(Entity entity, Integer prioThreshold);

    List<StanzaReceivingSessionContext> getHighestPrioSessions(Entity entity, Integer prioThreshold);
}
