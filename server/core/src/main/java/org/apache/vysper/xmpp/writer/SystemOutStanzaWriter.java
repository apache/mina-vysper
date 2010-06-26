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
package org.apache.vysper.xmpp.writer;

import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * writes stanza to System.out
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class SystemOutStanzaWriter implements StanzaWriter {

    boolean isFirst = true;

    private String closingElement;

    public void writeXMLProlog() {
        System.out.println("<?xml version=\"1.0\"?>");
    }

    public void write(Stanza stanza) {
        Renderer renderer = new Renderer(stanza);
        System.out.print(renderer.getOpeningElement() + renderer.getElementContent());

        if (isFirst)
            closingElement = renderer.getClosingElement();
        else
            System.out.print(closingElement);

        isFirst = false;
    }

    public void close() {
        if (closingElement == null) {
            System.out.print(closingElement);
        }
        closingElement = null;
    }
}
