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

import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;

/**
 * a stanza plus the flags indicating which parts of the stanza are actually to be written.
 * this is especially useful when opening the stream, where the opening tag is never/not immediately closed.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StanzaWriteInfo {
    private Stanza stanza = null;

    private boolean writeProlog = true;

    private boolean writeOpeningElement = true;

    private boolean writeContent = true;

    private boolean writeClosingElement = true;

    public StanzaWriteInfo(Stanza stanza) {
        this.stanza = stanza;
    }

    public StanzaWriteInfo(Stanza stanza, boolean isStreamOpening) {
        this.stanza = stanza;
        this.writeProlog = isStreamOpening;
        this.writeClosingElement = !isStreamOpening;
    }

    public Stanza getStanza() {
        return stanza;
    }

    public boolean isWriteProlog() {
        return writeProlog;
    }

    public boolean isWriteOpeningElement() {
        return writeOpeningElement;
    }

    public boolean isWriteContent() {
        return writeContent;
    }

    public boolean isWriteClosingElement() {
        return writeClosingElement;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ prolog=").append(writeProlog);
        stringBuilder.append(", open=").append(writeOpeningElement);
        stringBuilder.append(", close=").append(writeClosingElement);
        stringBuilder.append(", content=").append(writeContent);
        stringBuilder.append(", stanza=[").append(DenseStanzaLogRenderer.render(stanza));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
