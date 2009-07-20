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

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * write stanzas to a stream or other output target
 * the writer must assure that the first, opening stanza is only closed
 * when the writer is closed
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface StanzaWriter {
    public final static String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    void write(Stanza stanza);

    void close();
}
