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
package org.apache.vysper.xmpp.extension.xep0065_socks;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045 Multi-user chat</a>.
 * 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Socks5IqHandler extends DefaultIQHandler {

    private static InetAddress DEFAULT_ADDRESS;
    static {
        try {
            DEFAULT_ADDRESS = InetAddress.getByAddress(new byte[]{0,0,0,0});
        } catch (UnknownHostException ignore) {;}
    }
    
    final Logger logger = LoggerFactory.getLogger(Socks5IqHandler.class);

    private Entity jid;
    private InetSocketAddress proxyAddress;
    
    private Socks5ConnectionsRegistry connections;

    public Socks5IqHandler(Entity jid, InetSocketAddress proxyAddress, Socks5ConnectionsRegistry connections) {
        this.jid = jid;
        this.proxyAddress = proxyAddress;
        this.connections = connections;
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query") && verifyInnerNamespace(stanza, NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS);
    }

    @Override
    protected List<Stanza> handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        /*
            C: <iq from='requester@example.com/foo'
                    id='uj2c15z9'
                    to='streamer.example.com'
                    type='get'>
                  <query xmlns='http://jabber.org/protocol/bytestreams'/>
               </iq>
            
            S: <iq from='streamer.example.com'
                    id='uj2c15z9'
                    to='requester@example.com/foo'
                    type='result'>
                  <query xmlns='http://jabber.org/protocol/bytestreams'>
                    <streamhost
                        host='24.24.24.1'
                        jid='streamer.example.com'
                        port='7625'/>
                  </query>
                </iq>
         */
        
        StanzaBuilder builder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
            .startInnerElement("query", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS)
            .startInnerElement("streamhost", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS);
        
        // if an address is provided on the proxy address, use that, else use the JID
        if(DEFAULT_ADDRESS.equals(proxyAddress.getAddress())) {
            builder.addAttribute("host", jid.getFullQualifiedName());
        } else {
            builder.addAttribute("host", proxyAddress.getHostName());
        }
        builder.addAttribute("jid", jid.getFullQualifiedName())
            .addAttribute("port", Integer.toString(proxyAddress.getPort()));
        
        return Collections.singletonList(builder.build());
    }

    @Override
    protected List<Stanza> handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, StanzaBroker stanzaBroker) {
        /*
            C: <iq from='requester@example.com/foo'
                    id='oqx6t1c9'
                    to='streamer.example.com'
                    type='set'>
                  <query xmlns='http://jabber.org/protocol/bytestreams'
                     sid='vxf9n471bn46'>
                    <activate>target@example.org/bar</activate>
                  </query>
               </iq>
               
            S: <iq from='streamer.example.com'
                    id='oqx6t1c9'
                    to='requester@example.com/foo'
                    type='result'/>
                    
                    
            SHA1 Hash of: (SID + Requester JID + Target JID)
         */
        
        try {
            XMLElement queryElm = stanza.getSingleInnerElementsNamed("query", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS);
            XMLElement activateElm = queryElm.getSingleInnerElementsNamed("activate", NamespaceURIs.XEP0065_SOCKS5_BYTESTREAMS);
            
            String sid = queryElm.getAttributeValue("sid");
            
            Entity target = EntityImpl.parse(activateElm.getInnerText().getText());
            
            Entity requester = stanza.getFrom();
            
            String hash = DigestUtils.shaHex(sid + requester.getFullQualifiedName() + target.getFullQualifiedName());
            
            if(connections.activate(hash)) {
                Stanza result = StanzaBuilder.createIQStanza(jid, requester, IQStanzaType.RESULT, stanza.getID()).build();
                return Collections.singletonList(result);
            } else {
                throw new RuntimeException("Pair not found");
            }
        } catch(Exception e) {
            return Collections.singletonList(ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza, StanzaErrorType.CANCEL, null, null, null));
        }
    }
}
