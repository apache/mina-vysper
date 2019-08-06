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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.muc;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.protocol.DelegatingStanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * @author Réda Housni Alaoui
 */
class MUCMessageStanzaBroker extends DelegatingStanzaBroker {
    private final ServerRuntimeContext serverRuntimeContext;

    private final SessionContext sessionContext;

    private final boolean isOutboundStanza;

    public MUCMessageStanzaBroker(StanzaBroker delegate, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, boolean isOutboundStanza) {
        super(delegate);
        this.serverRuntimeContext = serverRuntimeContext;
        this.sessionContext = sessionContext;
        this.isOutboundStanza = isOutboundStanza;
    }

    @Override
    public void write(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {
        super.write(receiver, archive(stanza), deliveryFailureStrategy);
    }

    @Override
    public void writeToSession(Stanza stanza) {
        super.writeToSession(archive(stanza));
    }

    public Stanza archive(Stanza stanza) {
        // TODO archive
        return stanza;
    }
}
