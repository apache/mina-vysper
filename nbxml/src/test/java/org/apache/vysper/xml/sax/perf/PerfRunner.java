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
package org.apache.vysper.xml.sax.perf;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.decoder.XMLElementListener;
import org.apache.vysper.xml.decoder.XMPPContentHandler;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PerfRunner {

    private static class CounterStanzaListener implements XMLElementListener {

        public int counter = 0;

        public void element(XMLElement element) {
            counter++;
        }
        
        public void close() {}
    }

    private static final String SINGLE_LEVEL_XML = "<child att='foo' att2='bar'></child>";

    private static final String NESTED_XML = "<child att='foo' att2='bar'><child2><child3><child4></child4></child3></child2></child>";

    public static void main(String[] args) throws Exception {

        IoBuffer opening = IoBuffer.wrap("<p:root xmlns:p='http://example.com'>".getBytes("UTF-8"));
        IoBuffer buffer = IoBuffer.wrap(SINGLE_LEVEL_XML.getBytes("UTF-8"));

        DefaultNonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();
        CounterStanzaListener listener = new CounterStanzaListener();
        XMPPContentHandler contentHandler = new XMPPContentHandler();
        contentHandler.setListener(listener);
        reader.setContentHandler(contentHandler);

        StopWatch watch = new StopWatch();

        reader.parse(opening, CharsetUtil.getDecoder());
        for (int i = 0; i < 10000; i++) {
            buffer.position(0);
            reader.parse(buffer, CharsetUtil.getDecoder());
        }
        watch.stop();

        System.out.println(listener.counter + " stanzas parsed");
        System.out.println(watch);

    }

}
