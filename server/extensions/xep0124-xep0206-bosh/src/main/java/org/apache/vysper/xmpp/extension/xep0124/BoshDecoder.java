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
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Decodes bytes into BOSH requests
 * <p>
 * Uses nbxml for XML processing.
 * For every HTTP request there is a BoshDecoder instance
 * to ensure that parsing errors (e.g. malformed XML) do not affect other requests.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshDecoder {

    private final NonBlockingXMLReader reader;

    private final HttpServletRequest request;

    /**
     * Creates a new decoder to parse an HTTP request
     * @param boshHandler
     * @param req
     */
    public BoshDecoder(BoshHandler boshHandler, HttpServletRequest req) {
        request = req;
        reader = new DefaultNonBlockingXMLReader();
        ContentHandler contentHandler = new BoshSaxContentHandler(boshHandler, req);
        reader.setContentHandler(contentHandler);
    }

    /**
     * Decodes the bytes from the {@link InputStream} provided by the current {@link HttpServletRequest} of
     * the request context into a BOSH requests.
     * @throws IOException
     * @throws SAXException
     */
    public void decode() throws IOException, SAXException {
        IoBuffer ioBuf = IoBuffer.allocate(1024);
        ioBuf.setAutoExpand(true);
        byte[] buf = new byte[1024];
        InputStream in = request.getInputStream();

        for (;;) {
            int i = in.read(buf);
            if (i == -1) {
                break;
            }
            ioBuf.put(buf, 0, i);
        }
        ioBuf.flip();
        reader.parse(ioBuf, CharsetUtil.getDecoder());
    }

}