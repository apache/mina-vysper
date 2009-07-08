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

import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.MessageHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.StreamStartHandler;
import org.apache.vysper.xmpp.modules.core.base.handler.XMLPrologHandler;
import org.apache.vysper.xmpp.modules.core.im.handler.PresenceHandler;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * for effeciently looking up the right handler for a stanza. at first this class tries to determine the stanza's
 * specific namespace which uniquely brings up a NamespaceHandlerDictionary. then, all handlers in this directory
 * are visited and can verify if they might want to handle the stanza. the first affirmative handler lucks out and
 * can handle the stanza. regardless what comes out of this handler, no other handler will then be tasked with
 * handling.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaHandlerLookup {

    private Map<String, NamespaceHandlerDictionary> dictionaries = new LinkedHashMap<String, NamespaceHandlerDictionary>();

    private IQHandler iqHandler = new IQHandler();
    private MessageHandler messageHandler = new MessageHandler();
    private PresenceHandler presenceHandler = new PresenceHandler();
    private static final ServiceUnavailableStanzaErrorHandler SERVICE_UNAVAILABLE_STANZA_ERROR_HANDLER = new ServiceUnavailableStanzaErrorHandler();

    public void addDictionary(NamespaceHandlerDictionary namespaceHandlerDictionary) {
        String namespace = namespaceHandlerDictionary.getNamespaceURI();
        if (dictionaries.containsKey(namespace)) throw new IllegalArgumentException("dictionary already exists covering namespace " + namespace);
        dictionaries.put(namespace, namespaceHandlerDictionary);
    }

    /**
     * looks into the stanza to see which handler is responsible, if any
     * @param stanza
     * @return NULL, if no handler could be
     */
    public StanzaHandler getHandler(Stanza stanza) {
        if (stanza == null) return null;

        String name = stanza.getName();
        if      ("xml".equals(name)) return new XMLPrologHandler();
        else if ("stream".equals(name)) return new StreamStartHandler();
        else if (iqHandler.verify(stanza)) return getIQHandler(stanza);
        else if (messageHandler.verify(stanza)) return getMessageHandler(stanza);
        else if (presenceHandler.verify(stanza)) return getPresenceHandler(stanza);
        else {
            // this is not a core stanza (RFC3920), but something like the following
            // (in descending-probability order):
            // a. a custom extension of iq, message, presence
            // b. some handshake stanza other than iq, message, presence
            // c. an arbitrary test stanza
            // d. an evil forged stanza
            // e. some extension we don't know yet
            // ...so we delegate:
            return getHandlerForElement(stanza, stanza);
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

        if (stanza.getVerifier().subElementsPresentExact(1)) {
            XMLElement firstInnerElement = stanza.getFirstInnerElement();
            handlerForElement = getHandlerForElement(stanza, firstInnerElement);
        } else {
            // if no specialized handler can be identified, return general handler
            return iqHandler;
        }

        return handlerForElement;
    }

    /**
     * tries to find the handler by trying
     * 1. value of xmlElement's XMLNS attribute, if unique
     * 2. xmlElements namespace, if the element name has a namespace prefix
     */
    private StanzaHandler getHandlerForElement(Stanza stanza, XMLElement xmlElement) {

        // TODO review this method if it checks the right things
        String namespace = xmlElement.getVerifier().getUniqueXMLNSValue();
        NamespaceHandlerDictionary namespaceHandlerDictionary = dictionaries.get(namespace);

        // another try to get a dictionary
        if (namespaceHandlerDictionary == null) {
            namespace = xmlElement.getNamespacePrefix();
            namespaceHandlerDictionary = dictionaries.get(namespace);
        }
        if (namespaceHandlerDictionary != null) return namespaceHandlerDictionary.get(stanza);

        if (XMPPCoreStanza.getWrapper(stanza) != null) return SERVICE_UNAVAILABLE_STANZA_ERROR_HANDLER;

        return null;
    }

}
