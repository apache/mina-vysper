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
package org.apache.vysper.xmpp.extension.xep0124;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.mina.codec.StanzaBuilderFactory;
import org.apache.vysper.xml.decoder.XMPPContentHandler;
import org.apache.vysper.xml.decoder.XMPPContentHandler.StanzaListener;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.xml.sax.SAXException;

/**
 * Decodes bytes into BOSH stanzas
 * <p>
 * Uses nbxml for XML processing.
 * Every HTTP session has its own instance of BoshDecoder
 * (because decoding state is associated with the decoder, and to ensure that
 * the decoding errors of a session do not affect another session).
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshDecoder implements StanzaListener {

    private final BoshHandler boshHandler;

    private final NonBlockingXMLReader reader;

    private final BoshBackedSessionContext sessionContext;

    /**
     * Creates a new decoder associated with a {@link SessionContext}
     * @param boshHandler
     * @param sessionContext
     */
    public BoshDecoder(BoshHandler boshHandler,
            BoshBackedSessionContext sessionContext) {
        this.boshHandler = boshHandler;
        this.sessionContext = sessionContext;
        reader = new DefaultNonBlockingXMLReader();
        XMPPContentHandler contentHandler = new XMPPContentHandler(
                new StanzaBuilderFactory());
        contentHandler.setListener(this);
        reader.setContentHandler(contentHandler);
    }

    /**
     * Decodes the bytes from the {@link InputStream} provided by the current {@link HttpServletRequest} of
     * the session context into BOSH stanzas.
     * @throws IOException
     * @throws SAXException
     */
    public void decode() throws IOException, SAXException {
        IoBuffer ioBuf = IoBuffer.allocate(1024);
        ioBuf.setAutoExpand(true);
        byte[] buf = new byte[1024];
        InputStream in = sessionContext.getHttpRequest().getInputStream();

        for (;;) {
            int i = in.read(buf);
            if (i == -1) {
                break;
            }
            ioBuf.put(buf, 0, i);
        }
        ioBuf.flip();
        reader.parse(ioBuf, CharsetUtil.UTF8_DECODER);
    }

    public void stanza(XMLElement element) {
        boshHandler.processStanza(sessionContext, (Stanza) element);
    }

}