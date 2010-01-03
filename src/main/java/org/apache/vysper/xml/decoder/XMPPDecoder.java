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
package org.apache.vysper.xml.decoder;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.decoder.XMPPContentHandler.StanzaListener;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;

/**
 * splits xml stream into handy tokens for further processing
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMPPDecoder extends CumulativeProtocolDecoder {

    public static final String SESSION_ATTRIBUTE_NAME = "xmppParser";

    private XMLElementBuilderFactory builderFactory = new XMLElementBuilderFactory();
    
    public XMPPDecoder() {
    	// default constructor
    }

    public XMPPDecoder(XMLElementBuilderFactory builderFactory) {
    	this.builderFactory = builderFactory;
    }

    public static class MinaStanzaListener implements StanzaListener {
    	private ProtocolDecoderOutput protocolDecoder;
    	
		public MinaStanzaListener(ProtocolDecoderOutput protocolDecoder) {
			this.protocolDecoder = protocolDecoder;
		}

		public void stanza(XMLElement element) {
			protocolDecoder.write(element);
		}
    }
    
    @Override
    public boolean doDecode(IoSession ioSession, ByteBuffer byteBuffer, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {

    	NonBlockingXMLReader reader = (NonBlockingXMLReader) ioSession.getAttribute(SESSION_ATTRIBUTE_NAME);
    	
        if (reader == null) {
        	reader = new DefaultNonBlockingXMLReader();
        	reader.setContentHandler(new XMPPContentHandler(builderFactory));
        	
            ioSession.setAttribute(SESSION_ATTRIBUTE_NAME, reader);
        }
        
        XMPPContentHandler contentHandler = (XMPPContentHandler) reader.getContentHandler();
        contentHandler.setListener(new MinaStanzaListener(protocolDecoderOutput));
    	
        reader.parse(byteBuffer, CharsetUtil.UTF8_DECODER);
    	
        // we have parsed what we got, invoke again when more data is available
        return false;
    }
}