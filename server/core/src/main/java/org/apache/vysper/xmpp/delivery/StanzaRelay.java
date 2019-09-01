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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * receives a stanza and relays to the receiving entity
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface StanzaRelay {

    /**
     * relaying a stanza
     *
     * @param sessionContext
     *            The current session context. Can be null.
     * @param receiver
     *            the stanza receiver
     * @param stanza
     *            the payload
     * @param deliveryFailureStrategy
     *            what to do in case of errors
     * @throws DeliveryException
     *             error while relaying
     */
    void relay(InternalSessionContext sessionContext, Entity receiver, Stanza stanza,
			   DeliveryFailureStrategy deliveryFailureStrategy) throws DeliveryException;

    /**
     * @return TRUE iff the relay is live (started and not stopped)
     */
    boolean isRelaying();

    /**
     * Shutdown this relay and prevent it from accepting any further stanzas.
     */
    void stop();
}
