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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
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

	private static final String XML_DECL = "<?xml";
    private static final String STREAM_STREAM = "<stream:stream";

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
			if(element.getName().equals("stream")) {
				// reset the reader 
			}
			
			protocolDecoder.write(element);
		}
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
	protected boolean doDecode(IoSession session,
			IoBuffer in, ProtocolDecoderOutput out) throws Exception {
    	NonBlockingXMLReader reader = (NonBlockingXMLReader) session.getAttribute(SESSION_ATTRIBUTE_NAME);
    	
    	// peek to find XML stream resets
    	// TODO this is a bit ugly, revisit
    	in.mark();
    	String peek = in.getString(14, CharsetUtil.UTF8_DECODER);
    	in.reset();

    	if (reader == null || STREAM_STREAM.equals(peek) || (peek != null && peek.startsWith(XML_DECL))) {
        	reader = new DefaultNonBlockingXMLReader();
        	
        	// we need to check the jabber:client/jabber:server NS declarations
        	reader.setFeature(DefaultNonBlockingXMLReader.FEATURE_NAMESPACE_PREFIXES, true);
        	reader.setContentHandler(new XMPPContentHandler(builderFactory));
        	
        	session.setAttribute(SESSION_ATTRIBUTE_NAME, reader);
        }
        
        XMPPContentHandler contentHandler = (XMPPContentHandler) reader.getContentHandler();
        contentHandler.setListener(new MinaStanzaListener(out));
    	
        reader.parse(in, CharsetUtil.UTF8_DECODER);
    	
        // we have parsed what we got, invoke again when more data is available
        return false;
	}
}