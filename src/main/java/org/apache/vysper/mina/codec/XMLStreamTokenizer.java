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

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;
import org.apache.vysper.xmpp.xmldecoder.DecodingException;
import org.apache.vysper.xmpp.xmldecoder.ParticleDecoder;
import org.apache.vysper.xmpp.xmldecoder.XMLParticle;
import org.apache.vysper.xmpp.xmldecoder.XMLRawToFragmentConverter;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * splits xml stream into handy tokens for further processing
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLStreamTokenizer extends CumulativeProtocolDecoder {

    public static final String SESSION_ATTRIBUTE_NAME = "tokenizerParticleList";
    private static final XMLRawToFragmentConverter CONVERTER = new XMLRawToFragmentConverter();

    final Logger clientStanzaLogger = LoggerFactory.getLogger("stanza.client");

    @Override
    public boolean doDecode(IoSession ioSession, IoBuffer byteBuffer, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {

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

                    XMLFragment xmlFragment = CONVERTER.convert(particles);

                    if (xmlFragment instanceof XMLElement) {
                        // propagate element

                        XMLElement element = (XMLElement) xmlFragment;
                        clientStanzaLogger.info(DenseStanzaLogRenderer.render(element));
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