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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PubSubServiceConfiguration;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * This class is the superclass for all stanzas within the pubsub namespace.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public abstract class AbstractPubSubGeneralHandler extends AbstractPublishSubscribeIQHandler {

    /**
     * Initializes the abstract handler with the given configuration object.
     *
     * @param serviceConfiguration
     */
    public AbstractPubSubGeneralHandler(PubSubServiceConfiguration serviceConfiguration) {
        super(serviceConfiguration);
    }

    /**
     * @return the fixed pubsub namespace
     */
    @Override
    protected String getNamespace() {
        return NamespaceURIs.XEP0060_PUBSUB;
    }
}
