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
package org.apache.vysper.xmpp.modules.extension.xep0077_inbandreg;

import java.util.List;

import org.apache.vysper.xmpp.modules.DefaultModule;
import org.apache.vysper.xmpp.protocol.DefaultHandlerDictionary;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;

/**
 * A module for <a href="http://xmpp.org/extensions/xep-0077.html">XEP-0077 In-Band Registration</a>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class InBandRegistrationModule extends DefaultModule {

    private InBandRegistrationHandler handler = new InBandRegistrationHandler();
    
    @Override
    public String getName() {
        return "XEP-0077 In-Band Registration";
    }

    @Override
    public String getVersion() {
        return "2.3";
    }

    @Override
    protected void addHandlerDictionaries(List<HandlerDictionary> dictionary) {
        dictionary.add(new DefaultHandlerDictionary(handler));
    }
}
