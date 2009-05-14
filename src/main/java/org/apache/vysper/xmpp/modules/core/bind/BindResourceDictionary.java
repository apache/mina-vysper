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
package org.apache.vysper.xmpp.modules.core.bind;

import org.apache.vysper.xmpp.modules.core.bind.handler.BindIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * handlers for the urn:ietf:params:xml:ns:xmpp-bind namespace
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BindResourceDictionary extends NamespaceHandlerDictionary {

    public BindResourceDictionary() {
        super(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND);
        register(new BindIQHandler());
        seal();
    }
}