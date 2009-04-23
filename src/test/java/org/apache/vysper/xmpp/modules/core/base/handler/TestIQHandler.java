/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.vysper.xmpp.modules.core.base.handler;

import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.IQStanzaType;

public class TestIQHandler extends IQHandler {

    String name = null;

    private IQStanza incomingStanza;
    private String namespaceURI;

    public TestIQHandler() {
        // empty
    }

    public TestIQHandler(String name, String namespaceURI) {
        this.name = name;
        this.namespaceURI = namespaceURI;
    }

    @Override
    public boolean verify(Stanza stanza) {
        if (!super.verify(stanza)) return false;
        if (name == null) return true;
        return stanza.getVerifier().onlySubelementEquals(name, namespaceURI);
    }

    @Override
    protected Stanza executeIQLogic(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        incomingStanza = stanza;

        StanzaBuilder responseBuilder = new StanzaBuilder("iq", stanza.getNamespace());
        if (stanza.getID() != null) responseBuilder.addAttribute("id", stanza.getID());

        responseBuilder.addAttribute("type", IQStanzaType.RESULT.value());

         return responseBuilder.getFinalStanza();
    }

    public IQStanza getIncomingStanza() {
        return incomingStanza;
    }
}
