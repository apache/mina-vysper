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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XTestCase extends TestCase {

    private static Entity JID = EntityImpl.parseUnchecked("jid1@vysper.org");

    public void testFromStanza() {
        StanzaBuilder builder = StanzaBuilder.createMessageStanza(JID, JID, null, "Foo");
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC);
        builder.startInnerElement("password", NamespaceURIs.XEP0045_MUC).addText("secret").endInnerElement();
        builder.endInnerElement();

        X x = X.fromStanza(builder.build());

        assertNotNull(x);
        assertEquals("secret", x.getPasswordValue());
    }
}
