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
package org.apache.vysper.xmpp.modules.core.starttls;

import org.apache.vysper.xmpp.modules.core.starttls.handler.StartTLSHandler;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * handler for stream securing (TLS) stanzas from RFC3920 (xmpp core)<br/>
 * they are: starttls, failure, proceed
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StartTLSStanzaDictionary extends NamespaceHandlerDictionary {

    public StartTLSStanzaDictionary() {
        super(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_TLS);
        register(new StartTLSHandler());
        seal();
    }
}