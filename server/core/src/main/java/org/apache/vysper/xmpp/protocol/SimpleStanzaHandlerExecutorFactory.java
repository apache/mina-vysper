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
package org.apache.vysper.xmpp.protocol;

import static java.util.Objects.requireNonNull;

import org.apache.vysper.xmpp.delivery.OfflineStanzaReceiver;
import org.apache.vysper.xmpp.delivery.StanzaRelay;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleStanzaHandlerExecutorFactory implements StanzaHandlerExecutorFactory {

    private final StanzaRelay stanzaRelay;

    private final OfflineStanzaReceiver offlineStanzaReceiver;

    public SimpleStanzaHandlerExecutorFactory(StanzaRelay stanzaRelay, OfflineStanzaReceiver offlineStanzaReceiver) {
        this.stanzaRelay = requireNonNull(stanzaRelay);
        this.offlineStanzaReceiver = offlineStanzaReceiver;
    }
    
    public SimpleStanzaHandlerExecutorFactory(StanzaRelay stanzaRelay){
        this(stanzaRelay, null);
    }

    @Override
    public StanzaHandlerExecutor build(StanzaHandler stanzaHandler) {
        return new SimpleStanzaHandlerExecutor(stanzaRelay, stanzaHandler, offlineStanzaReceiver);
    }

}
