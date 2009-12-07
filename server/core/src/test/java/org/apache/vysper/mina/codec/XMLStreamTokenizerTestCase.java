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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.decoder.XMLStreamTokenizer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.writer.StanzaWriter;

/**
 */
public class XMLStreamTokenizerTestCase extends TestCase {
    private static final CharsetEncoder CHARSET_ENCODER_UTF8 = CharsetUtil.UTF8_ENCODER;

    public void testDecoderSimple() throws Exception {
        XMLStreamTokenizer decoder = new XMLStreamTokenizer();
        MockIoSession session = new MockIoSession();
        ByteBuffer firstByteBuffer = createByteBuffer();
        ByteBuffer secondByteBuffer = createByteBuffer();
        
        String stanza = StanzaWriter.XML_PROLOG + "\n\r" + 
                "<stream:stream to='example.com' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>" +
                "<trailing-stanza/>";

        firstByteBuffer.putString(stanza, CHARSET_ENCODER_UTF8).flip();
        
        MockProtocolDecoderOutput protocolDecoderOutput = new MockProtocolDecoderOutput();
        decoder.decode(session, firstByteBuffer, protocolDecoderOutput);
        assertEquals(4, protocolDecoderOutput.size());
        
        secondByteBuffer.putString("<next></next>", CHARSET_ENCODER_UTF8).flip();
        
        decoder.decode(session, secondByteBuffer, protocolDecoderOutput);
        assertEquals(5, protocolDecoderOutput.size());

        ByteBuffer emptyBuffer = createByteBuffer().putString("eee", CHARSET_ENCODER_UTF8).flip();
        decoder.decode(session, emptyBuffer, protocolDecoderOutput);
        assertEquals("plain must be terminated by <", 5, protocolDecoderOutput.size());
        
        ByteBuffer termBuffer = createByteBuffer().putString("<r>", CHARSET_ENCODER_UTF8).flip();
        decoder.decode(session, termBuffer, protocolDecoderOutput);
        // eee is now terminated, but r is not balanced yet
        assertEquals("plain termination", 6, protocolDecoderOutput.size());
        
    }

    public void testDecoderPartial() throws Exception {
        XMLStreamTokenizer decoder = new XMLStreamTokenizer();
        MockIoSession session = new MockIoSession();
        ByteBuffer firstByteBuffer = createByteBuffer();
        ByteBuffer secondByteBuffer = createByteBuffer();
        
        String stanzaPart1 = "<stream:stream to='example.com' xmlns='jabber:client' xmlns:stream='ht";
        String stanzaPart2 = "tp://etherx.jabber.org/streams' version='1.0'>";

        MockProtocolDecoderOutput protocolDecoderOutput = new MockProtocolDecoderOutput();

        ByteBuffer prolog = createByteBuffer();
        prolog.putString(StanzaWriter.XML_PROLOG + "\n\r", CHARSET_ENCODER_UTF8).flip();
        decoder.decode(session, prolog, protocolDecoderOutput);
        assertEquals(1, protocolDecoderOutput.size());
        
        firstByteBuffer.putString(stanzaPart1, CHARSET_ENCODER_UTF8).flip();
        decoder.decode(session, firstByteBuffer, protocolDecoderOutput);
        assertEquals(2, protocolDecoderOutput.size());
        String content = ((XMLText)protocolDecoderOutput.get(1)).getText();
        assertEquals("\n\r", content);


        secondByteBuffer.putString(stanzaPart2, CHARSET_ENCODER_UTF8).flip();
        decoder.decode(session, secondByteBuffer, protocolDecoderOutput);
        assertEquals(3, protocolDecoderOutput.size());
    }

    public void testCRLFInElement() throws Exception {
        XMLStreamTokenizer decoder = new XMLStreamTokenizer();
        MockIoSession session = new MockIoSession();
        ByteBuffer byteBuffer = createByteBuffer();
        
        String stanza = 
        "<stream:stream\n" +
                "     from='juliet@example.com'\n" +
                "     to='example.com'\n" +
                "     version='1.0'\n" +
                "     xml:lang='en'\n" +
                "     xmlns='jabber:client'\n" +
                "     xmlns:stream='http://etherx.jabber.org/streams'>";
        
        MockProtocolDecoderOutput protocolDecoderOutput = new MockProtocolDecoderOutput();

        byteBuffer.putString(stanza, CHARSET_ENCODER_UTF8).flip();
        try {
            decoder.decode(session, byteBuffer, protocolDecoderOutput);
        } catch(Throwable th) {
            int lkjl = 0;
        }
        assertEquals(1, protocolDecoderOutput.size());
        XMLElement stanzaParsed = (XMLElement) protocolDecoderOutput.get(0);
        String stanzaName = stanzaParsed.getName();
        assertEquals("stream", stanzaName);
        String stanzaNSPrefix = stanzaParsed.getNamespacePrefix();
        assertEquals("stream", stanzaNSPrefix);
    }

    private ByteBuffer createByteBuffer() {
        return ByteBuffer.allocate(0, false).setAutoExpand(true).clear();
    }

    protected class MockProtocolDecoderOutput implements ProtocolDecoderOutput
    {
        private List<XMLFragment> result = new ArrayList<XMLFragment>();

        public void flush()
        {
        }

        public void write( Object message )
        {
            result.add((XMLFragment) message);
        }
        
        public Iterator<XMLFragment> iterator() {
            return result.iterator();
        }

        public XMLFragment get(int i) {
            return result.get(i);
        }
        
        public int size() {
            return result.size();
        }
    }    
}

