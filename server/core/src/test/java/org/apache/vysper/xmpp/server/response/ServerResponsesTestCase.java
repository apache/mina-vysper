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

package org.apache.vysper.xmpp.server.response;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.authorization.Anonymous;
import org.apache.vysper.xmpp.authorization.External;
import org.apache.vysper.xmpp.authorization.Plain;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.parser.ParsingException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class ServerResponsesTestCase extends TestCase {

    public void testFeaturesForAuthentication() throws ParsingException {

        Stanza stanza = new StanzaBuilder("features")
        	.startInnerElement("mechanisms", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)
        		.startInnerElement("mechanism", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText("EXTERNAL").endInnerElement()
        		.startInnerElement("mechanism", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText("PLAIN").endInnerElement()
        		.startInnerElement("mechanism", NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).addText("ANONYMOUS").endInnerElement()
        	.endInnerElement()
        	.build();


        List<SASLMechanism> mechanismList = new ArrayList<SASLMechanism>();
        mechanismList.add(new External());
        mechanismList.add(new Plain());
        mechanismList.add(new Anonymous());
        // add others
        assertEquals("stanzas are identical", stanza.toString(), new ServerResponses().getFeaturesForAuthentication(mechanismList).toString());
    }
}
