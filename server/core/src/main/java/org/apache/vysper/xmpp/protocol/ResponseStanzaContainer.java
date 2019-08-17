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
package org.apache.vysper.xmpp.protocol;

import java.util.List;

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * used to carry a response stanza
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface ResponseStanzaContainer {

    List<Stanza> getResponseStanzas();

    /**
     * @return If there is exactly one response, the unique response. If there is no
     *         response it will return null. If there is more than one response, it
     *         will throw an exception.
     */
    Stanza getUniqueResponseStanza();

    boolean hasResponse();

    boolean hasNoResponse();
}
