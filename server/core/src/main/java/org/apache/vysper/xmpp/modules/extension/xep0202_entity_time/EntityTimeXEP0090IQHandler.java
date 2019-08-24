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
package org.apache.vysper.xmpp.modules.extension.xep0202_entity_time;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 * implements deprecated XEP0090 Entity Time
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0090", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class EntityTimeXEP0090IQHandler extends DefaultIQHandler {

    private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

    protected SimpleDateFormat utcDateFormatter;

    protected SimpleDateFormat localDateFormatter;

    public EntityTimeXEP0090IQHandler() {
        localDateFormatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);

        utcDateFormatter = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
        utcDateFormatter.setTimeZone(TIME_ZONE_UTC); // convert to UTC
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query") && verifyInnerNamespace(stanza, NamespaceURIs.JABBER_IQ_TIME);
    }

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {

        Date now = new Date();

        String timeZone = TimeZone.getDefault().getDisplayName(TimeZone.getDefault().inDaylightTime(now),
                TimeZone.SHORT);
        String utcTime = utcDateFormatter.format(now);
        String displayTime = localDateFormatter.format(now);

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(),
                IQStanzaType.RESULT, stanza.getID()).startInnerElement("query", NamespaceURIs.JABBER_IQ_TIME).

        startInnerElement("utc", NamespaceURIs.JABBER_IQ_TIME).addText(utcTime).endInnerElement().startInnerElement(
                "tz", NamespaceURIs.JABBER_IQ_TIME).addText(timeZone).endInnerElement().startInnerElement("display",
                NamespaceURIs.JABBER_IQ_TIME).addText(displayTime).endInnerElement().

        endInnerElement();

        return Collections.singletonList(stanzaBuilder.build());
    }
}
