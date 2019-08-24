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
package org.apache.vysper.xmpp.modules.extension.xep0199_xmppping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0199.html">XEP-0199 XMPP Ping</a>.
 * 
 * The specification defines several modes, where client to server is currently the only mode
 * implemented here. An wire protocol example of a client to server ping: 
 * <pre>
 * C: &lt;iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='s2c1' type='get'&gt;
 *      &lt;ping xmlns='urn:xmpp:ping'/&gt;
 *    &lt;/iq&gt;
 *
 * S: &lt;iq from='juliet@capulet.lit/balcony' to='capulet.lit' id='s2c1' type='result'/&gt;
 * </pre>
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0199", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL)
public class XmppPingIQHandler extends DefaultIQHandler {

    private static final Logger LOG = LoggerFactory.getLogger(XmppPingIQHandler.class);
    
    private final List<XmppPinger> pingers = new ArrayList<XmppPinger>();
    
    public XmppPingIQHandler() {
    }

    @Override
    public boolean verify(Stanza stanza) {
        if(stanza == null) return false;
        
        boolean extension = super.verify(stanza);
        if(extension) {
            return true;
        } else {
            // handle result stanzas, which does not contain the extension element
            String type = stanza.getAttributeValue("type");
            if(type != null && type.equals("result")) {
                String id = stanza.getAttributeValue("id");
                if(id != null && id.startsWith("xmppping-")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    protected void addPinger(XmppPinger pinger) {
        pingers.add(pinger);
    }

    protected void removePinger(XmppPinger pinger) {
        pingers.remove(pinger);
    }
    
    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "ping") && verifyInnerNamespace(stanza, NamespaceURIs.URN_XMPP_PING);
    }

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(),
                IQStanzaType.RESULT, stanza.getID());

        return Collections.singletonList(stanzaBuilder.build());
    }
    
    @Override
    protected List<Stanza> handleResult(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
										SessionContext sessionContext, StanzaBroker stanzaBroker) {
        List<XmppPinger> pingersCopy = new ArrayList<XmppPinger>(pingers);
        for (XmppPinger pinger : pingersCopy) {
            try {
                pinger.pong(stanza.getID());
            } catch (Throwable e) {
                LOG.warn("ponging the pinger produced problem: " + e.getMessage());
                LOG.debug("ponging the pinger produced problem. ", e);
            }
        }

        return Collections.emptyList();
    }
    
    
}
