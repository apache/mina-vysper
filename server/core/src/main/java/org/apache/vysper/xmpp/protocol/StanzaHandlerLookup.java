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

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.MessageHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.RelayingIQHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.StreamStartHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.XMLPrologHandler;
import org.apache.vysper.xmpp.modules.core.im.handler.PresenceHandler;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DbResultHandler;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DbVerifyHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * for effeciently looking up the right handler for a stanza. at first this class tries to determine the stanza's
 * specific namespace which uniquely brings up a NamespaceHandlerDictionary. then, all handlers in this directory
 * are visited and can verify if they might want to handle the stanza. the first affirmative handler lucks out and
 * can handle the stanza. regardless what comes out of this handler, no other handler will then be tasked with
 * handling.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaHandlerLookup extends AbstractStanzaHandlerLookup {

    private IQHandler iqHandler = new RelayingIQHandler();

    private MessageHandler messageHandler = new MessageHandler();

    private PresenceHandler presenceHandler = new PresenceHandler();

    private static final ServiceUnavailableStanzaErrorHandler SERVICE_UNAVAILABLE_STANZA_ERROR_HANDLER = new ServiceUnavailableStanzaErrorHandler();

    protected ServerRuntimeContext serverRuntimeContext;

    public StanzaHandlerLookup(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /**
     * looks into the stanza to see which handler is responsible, if any
     * @param stanza
     * @return NULL, if no handler could be
     */
    @Override
    public StanzaHandler getHandler(Stanza stanza) {
        if (stanza == null)
            return null;

        // allow extensions to override default handling
        StanzaHandler stanzaHandler = getHandlerForElement(stanza, stanza);
        
        if(stanzaHandler != null) {
            return stanzaHandler;
        } else {
            String name = stanza.getName();
    
            if ("xml".equals(name)) {
                return new XMLPrologHandler();
            } else if ("stream".equals(name)) {
                return new StreamStartHandler();
            } else if ("verify".equals(name)) {
                return new DbVerifyHandler();
            } else if ("result".equals(name)) {
                return new DbResultHandler();
            } else if (iqHandler.verify(stanza)) {
                return getIQHandler(stanza);
            } else if (messageHandler.verify(stanza)) {
                return getMessageHandler(stanza);
            } else if (presenceHandler.verify(stanza)) {
                return getPresenceHandler(stanza);
            } else {
                // ... and if we could not resolve and it's a core stanza, we can safely return an error
                if (XMPPCoreStanza.getWrapper(stanza) != null) return SERVICE_UNAVAILABLE_STANZA_ERROR_HANDLER;
                else return null;
            }
        }
    }

    private StanzaHandler getPresenceHandler(Stanza stanza) {
        return presenceHandler;
    }

    private StanzaHandler getMessageHandler(Stanza stanza) {
        return messageHandler;
    }

    private StanzaHandler getIQHandler(Stanza stanza) {
        StanzaHandler handlerForElement = null;

        Entity to = stanza.getTo();
        Entity serverEntity = (serverRuntimeContext == null) ? null : serverRuntimeContext.getServerEntity();
        boolean isAddressedToServerOrComponent = (to == null || (!to.isNodeSet() && !to.isResourceSet()));
        boolean isAddressedToComponent = (to != null) && isAddressedToServerOrComponent && serverEntity != null
                && (!serverEntity.equals(to));
        boolean isAddressedToServer = (to == null) || (isAddressedToServerOrComponent && !isAddressedToComponent);

        // The following cases must be properly handled:
        // 1. IQ disco stanza always handled by disco subsystem, not addressee
        // 2. to = someone@vysper.org => relay
        // 3. to = vysper.org => service unavailable
        // 4. to = component.vysper.org => relay

        // if no specialized handler can be identified, return general handler (relay)
        StanzaHandler resolvedHandler = null;

        if (stanza.getVerifier().subElementsPresentExact(1)) {
            XMLElement firstInnerElement = stanza.getFirstInnerElement();
            handlerForElement = getHandlerForElement(stanza, firstInnerElement);
            if (handlerForElement != null)
                resolvedHandler = handlerForElement;
            if (resolvedHandler == null && isAddressedToServer && XMPPCoreStanza.getWrapper(stanza) != null)
                resolvedHandler = SERVICE_UNAVAILABLE_STANZA_ERROR_HANDLER;
        }
        if (resolvedHandler == null)
            resolvedHandler = iqHandler;
        return resolvedHandler;
    }
}
