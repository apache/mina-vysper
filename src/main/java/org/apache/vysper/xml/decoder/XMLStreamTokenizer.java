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

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;

/**
 * splits xml stream into handy tokens for further processing
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLStreamTokenizer extends CumulativeProtocolDecoder {

    public static final String SESSION_ATTRIBUTE_NAME = "tokenizerParticleList";
    private static final XMLRawToFragmentConverter CONVERTER = new XMLRawToFragmentConverter();

    private XMLElementBuilderFactory builderFactory = new XMLElementBuilderFactory();
    
    public XMLStreamTokenizer() {
    	// default constructor
    }

    public XMLStreamTokenizer(XMLElementBuilderFactory builderFactory) {
    	this.builderFactory = builderFactory;
    }

    
    @Override
    public boolean doDecode(IoSession ioSession, ByteBuffer byteBuffer, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {

        XMLParticle xmlParticle = ParticleDecoder.decodeParticle(byteBuffer, CharsetUtil.UTF8_DECODER);

        if (xmlParticle != null) {
            // new particle is completed
            // get current list from session
            List<XMLParticle> particles = (List<XMLParticle>) ioSession.getAttribute(SESSION_ATTRIBUTE_NAME);
            if (particles == null) {
                particles = new ArrayList<XMLParticle>();
                ioSession.setAttribute(SESSION_ATTRIBUTE_NAME, particles);
            }

            // add to list of particles
            particles.add(xmlParticle);

            // only if the list is balanced, a new stanza is completed
            try {
                if (CONVERTER.isBalanced(particles)) {
                    // reset session list
                    ioSession.setAttribute(SESSION_ATTRIBUTE_NAME, new ArrayList<XMLParticle>());

                    XMLFragment xmlFragment = CONVERTER.convert(particles, builderFactory);

                    if (xmlFragment instanceof XMLElement) {
                        // propagate element
                    	
                        XMLElement element = (XMLElement) xmlFragment;
                        protocolDecoderOutput.write(element);
                    } else {
                        // TODO handle text elements properly might be only whitespaces/newlines
                        protocolDecoderOutput.write(xmlFragment);
                    }
                }
            } catch (DecodingException e) {
                throw e; // rethrow
            }

        }

        return xmlParticle != null;
    }
}