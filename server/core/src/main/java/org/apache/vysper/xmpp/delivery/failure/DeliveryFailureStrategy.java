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
package org.apache.vysper.xmpp.delivery.failure;

import java.util.List;

import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * there are many reasons why a stanza may fail to deliver: remote server not answering, local addressee has
 * become unavailable, the server has no more resources to process etc.
 * what to do in this case also depends on the context. if the server intends to send out a presence notification,
 * an unavailable client might be ignored, but if a message stanza fails to deliver, the sender might wants to be
 * notified.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface DeliveryFailureStrategy {

    /**
     *
     * @param failedToDeliverStanza - stanza which could not be delivered
     * @param deliveryException - optional: exception which occured during the failed delivery
     * @throws org.apache.vysper.xmpp.delivery.failure.DeliveryException - exception which occured during failure strategy execution.
     */
    public void process(Stanza failedToDeliverStanza, List<DeliveryException> deliveryException)
            throws DeliveryException;

}
