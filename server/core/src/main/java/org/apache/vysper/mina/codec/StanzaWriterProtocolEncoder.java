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
package org.apache.vysper.mina.codec;

import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.mina.XmppIoHandlerAdapter;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * connects MINA low level protocol and session stanza writer
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaWriterProtocolEncoder implements ProtocolEncoder {

    private final Logger logger = LoggerFactory.getLogger(StanzaWriterProtocolEncoder.class);

    public void encode(IoSession ioSession, Object o, ProtocolEncoderOutput protocolEncoderOutput) throws Exception {
        if (!(o instanceof StanzaWriteInfo)) {
            throw new IllegalArgumentException("StanzaWriterProtocolEncoder only handles StanzaWriteInfo objects");
        }
        StanzaWriteInfo stanzaWriteInfo = (StanzaWriteInfo) o;

        Stanza element = stanzaWriteInfo.getStanza();
        Renderer renderer = new Renderer(element);

        IoBuffer byteBuffer = IoBuffer.allocate(16).setAutoExpand(true);
        if (stanzaWriteInfo.isWriteProlog())
            byteBuffer.putString(StanzaWriter.XML_PROLOG, getSessionEncoder());
        if (stanzaWriteInfo.isWriteOpeningElement())
            byteBuffer.putString(renderer.getOpeningElement(), getSessionEncoder());
        if (stanzaWriteInfo.isWriteContent())
            byteBuffer.putString(renderer.getElementContent(), getSessionEncoder());
        if (stanzaWriteInfo.isWriteClosingElement())
            byteBuffer.putString(renderer.getClosingElement(), getSessionEncoder());

        byteBuffer.flip();
        protocolEncoderOutput.write(byteBuffer);
    }

    public void dispose(IoSession ioSession) throws Exception {
        final IoHandler handler = ioSession.getHandler();
        if (handler instanceof XmppIoHandlerAdapter) {
            XmppIoHandlerAdapter xmppIoHandlerAdapter = (XmppIoHandlerAdapter)handler;
            xmppIoHandlerAdapter.sessionClosed(ioSession);
            logger.debug("terminated and disposed session id = " + ioSession.getId());
        } else {
            logger.warn("unhandled StanzaWriterProtocolEncoder.dispose()");
        }
    }

    public CharsetEncoder getSessionEncoder() {
        return CharsetUtil.getEncoder();
    }

}
