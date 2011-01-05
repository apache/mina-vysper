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
package org.apache.vysper.xmpp.extension.websockets;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.mina.codec.StanzaBuilderFactory;
import org.apache.vysper.xml.decoder.XMPPContentHandler;
import org.apache.vysper.xml.decoder.XMPPContentHandler.StanzaListener;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.server.AbstractSessionContext;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class WebsocketsBackedSessionContext extends AbstractSessionContext implements WebSocket, StanzaListener, StanzaWriter {

    private final static Charset CHARSET = Charset.forName("UTF-8");
    private final static CharsetDecoder CHARSET_DECODER = CHARSET.newDecoder();
    
    private final static Logger LOG = LoggerFactory.getLogger(WebsocketsBackedSessionContext.class);

    private Outbound outbound;
    private NonBlockingXMLReader xmlReader = new DefaultNonBlockingXMLReader();
    
    public WebsocketsBackedSessionContext(ServerRuntimeContext serverRuntimeContext,
            SessionStateHolder sessionStateHolder) {
        super(serverRuntimeContext, sessionStateHolder);
        
        XMPPContentHandler contentHandler = new XMPPContentHandler(new StanzaBuilderFactory());
        contentHandler.setListener(this);
        
        try {
            // we need to check the jabber:client/jabber:server NS declarations
            xmlReader.setFeature(DefaultNonBlockingXMLReader.FEATURE_NAMESPACE_PREFIXES, true);
            // allow parser to restart XML stream
            xmlReader.setFeature(DefaultNonBlockingXMLReader.FEATURE_RESTART_ALLOWED, true);
            xmlReader.setProperty(DefaultNonBlockingXMLReader.PROPERTY_RESTART_QNAME, "stream:stream");
        } catch (SAXException e) {
            // should never happen
            throw new RuntimeException(e);
        }

        xmlReader.setContentHandler(contentHandler);
    }

    public StanzaWriter getResponseWriter() {
        return this;
    }

    public void switchToTLS(boolean delayed, boolean clientTls) {
        // n/a
    }

    public void setIsReopeningXMLStream() {
        // n/a
    }

    public void onConnect(Outbound outbound) {
        LOG.info("WebSocket client connected");
        this.outbound = outbound;
        
        // set to encrypted to skip TLS
        sessionStateHolder.setState(SessionState.ENCRYPTED);
    }

    public void onMessage(byte frame, String data) {
        LOG.info("< " + data);
        try {
            xmlReader.parse(IoBuffer.wrap(data.getBytes(CHARSET.name())), CHARSET_DECODER);
        } catch (IOException e) {
            // should never happen since we read from a string
            throw new RuntimeException(e);
        } catch (SAXException e) {
            Stanza errorStanza = ServerErrorResponses.getInstance().getStreamError(StreamErrorCondition.XML_NOT_WELL_FORMED,
                    getXMLLang(), "Stanza not well-formed", null);
            write(errorStanza);
            endSession(SessionTerminationCause.STREAM_ERROR);
        }
    }


    public void onDisconnect() {
        LOG.info("WebSocket client disconnected");
        endSession(SessionTerminationCause.CONNECTION_ABORT);
    }

    public void stanza(XMLElement element) {
        // on parsed stanzas
        serverRuntimeContext.getStanzaProcessor().processStanza(serverRuntimeContext, this, (Stanza) element, sessionStateHolder);
    }

    public void write(Stanza stanza) {
        // handle stream open
        Renderer renderer = new Renderer(stanza);
        String xml;
        if("stream".equals(stanza.getName()) && NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS.equals(stanza.getNamespaceURI())) {
            xml = renderer.getOpeningElement() + renderer.getElementContent();
        } else {
            xml = renderer.getComplete();
        }
        try {
            LOG.info("> " + xml);
            outbound.sendMessage(xml);
        } catch (IOException e) {
            // communication with client broken, close session
            endSession(SessionTerminationCause.CONNECTION_ABORT);
        }
    }

    public void close() {
        // TODO how to handle?
    }

    public void onMessage(byte frame, byte[] data, int offset, int length) {
        // binary data, should not happen, ignore
    }

    public void onFragment(boolean more, byte opcode, byte[] data, int offset, int length) {
        System.out.println("onFragment");
    }
}
