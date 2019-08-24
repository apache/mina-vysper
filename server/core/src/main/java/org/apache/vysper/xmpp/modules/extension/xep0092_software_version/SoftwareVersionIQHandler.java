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
package org.apache.vysper.xmpp.modules.extension.xep0092_software_version;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.Version;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0092", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class SoftwareVersionIQHandler extends DefaultIQHandler {

    public static final String VYSPER_RELEASE = Version.getVersion();

    public static final String OS_VERSION = System.getProperty("os.name", "undetermined") + " "
            + System.getProperty("os.arch", "") + " " + System.getProperty("os.version", "");

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query") && verifyInnerNamespace(stanza, NamespaceURIs.JABBER_IQ_VERSION);
    }

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(),
                IQStanzaType.RESULT, stanza.getID()).startInnerElement("query", NamespaceURIs.JABBER_IQ_VERSION).

        startInnerElement("name", NamespaceURIs.JABBER_IQ_VERSION).addText("Apache Vysper XMPP Server")
                .endInnerElement().startInnerElement("version", NamespaceURIs.JABBER_IQ_VERSION)
                .addText(VYSPER_RELEASE).endInnerElement().startInnerElement("os", NamespaceURIs.JABBER_IQ_VERSION)
                .addText(OS_VERSION).endInnerElement().

                endInnerElement();

        return Collections.singletonList(stanzaBuilder.build());
    }
}
