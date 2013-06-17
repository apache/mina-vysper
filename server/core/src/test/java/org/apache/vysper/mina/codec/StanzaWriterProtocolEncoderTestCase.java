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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;


/**
 */
public class StanzaWriterProtocolEncoderTestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from@vysper.org");
    private static final Entity TO = EntityImpl.parseUnchecked("vysper.org");
    
    private IoSession ioSession = Mockito.mock(IoSession.class);
    private ProtocolEncoderOutput output = Mockito.mock(ProtocolEncoderOutput.class);
    
    private StanzaWriterProtocolEncoder encoder = new StanzaWriterProtocolEncoder();
    
    private Stanza stanza = new StanzaBuilder("foo", "http://example.com")
        .startInnerElement("bar")
        .build();
    
    private String prolog = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private Renderer renderer = new Renderer(stanza);
    private String opening = renderer.getOpeningElement();
    private String content = renderer.getElementContent();
    private String closing = renderer.getClosingElement();
    
    @Test(expected=IllegalArgumentException.class)
    public void encodeNoneStanzaWriteInfo() throws Exception {
        encoder.encode(ioSession, "dummy", output);
    }

    @Test
    public void encode() throws Exception {
        StanzaWriteInfo writeInfo = new StanzaWriteInfo(stanza);
        
        encoder.encode(ioSession, writeInfo, output);
        
        ArgumentCaptor<IoBuffer> bufferCaptor = ArgumentCaptor.forClass(IoBuffer.class);
        
        Mockito.verify(output).write(bufferCaptor.capture());
        
        IoBuffer buffer = bufferCaptor.getValue();
        String actual = buffer.getString(CharsetUtil.getDecoder());
        
        Assert.assertEquals(prolog + opening + content + closing, actual);
    }

    @Test
    public void encodeStreamOpening() throws Exception {
        StanzaWriteInfo writeInfo = new StanzaWriteInfo(stanza, true);
        
        encoder.encode(ioSession, writeInfo, output);
        
        ArgumentCaptor<IoBuffer> bufferCaptor = ArgumentCaptor.forClass(IoBuffer.class);
        
        Mockito.verify(output).write(bufferCaptor.capture());
        
        IoBuffer buffer = bufferCaptor.getValue();
        String actual = buffer.getString(CharsetUtil.getDecoder());
        
        Assert.assertEquals(prolog + opening + content, actual);
    }

}
