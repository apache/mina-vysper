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
package org.apache.vysper.mina;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.vysper.mina.codec.StanzaWriteInfo;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaLoggingFilter extends IoFilterAdapter {

    final Logger serverLogger = LoggerFactory.getLogger("stanza.server");

    final Logger clientLogger = LoggerFactory.getLogger("stanza.client");

    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (message instanceof XMLElement) {
            XMLElement element = (XMLElement) message;

            if(clientLogger.isDebugEnabled()) {
                boolean openElement = true;
                boolean closeElement = true;
                // this is somewhat of a hack, can we detect opening and closing elements only cleaner?
                if (element.getName().equals("stream")) {
                    if (element.getAttributes().size() > 0) {
                        // is stream element, and with attributes, should be opening tag
                        closeElement = false;
                    } else {
                        // is stream element, without attributes, should be closing tag
                        openElement = false;
                    }
                }
                
                String xml = toXml(element, openElement, closeElement);
                clientLogger.debug("< " + xml);
            } else if (clientLogger.isInfoEnabled()) {
                clientLogger.info(DenseStanzaLogRenderer.render(element));
            }
        }

        nextFilter.messageReceived(session, message);
    }

    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception {
        Object message = request.getMessage();
        if (message instanceof StanzaWriteInfo) {
            StanzaWriteInfo stanzaWriteInfo = (StanzaWriteInfo) message;

            if(serverLogger.isDebugEnabled()) {
                String xml = toXml(stanzaWriteInfo.getStanza(), stanzaWriteInfo.isWriteOpeningElement(),
                        stanzaWriteInfo.isWriteClosingElement());
                
                serverLogger.debug("> " + xml);
            } else if (serverLogger.isInfoEnabled()) {
                serverLogger.info(DenseStanzaLogRenderer.render(stanzaWriteInfo.getStanza()));
            }
        }

        nextFilter.messageSent(session, request);
    }

    private String toXml(XMLElement element, boolean openElement, boolean closeElement) {
        Renderer renderer = new Renderer(element);

        StringBuffer xml = new StringBuffer();
        if (openElement) {
            xml.append(renderer.getOpeningElement());
        }
        xml.append(renderer.getElementContent());
        if (closeElement) {
            xml.append(renderer.getClosingElement());
        }
        return xml.toString();
    }
}
