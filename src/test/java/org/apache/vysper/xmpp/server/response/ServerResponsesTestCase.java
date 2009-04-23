/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.vysper.xmpp.server.response;

import org.apache.vysper.xmpp.parser.ParsingException;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.parser.StringStreamParser;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.authorization.Anonymous;
import org.apache.vysper.xmpp.authorization.Plain;
import org.apache.vysper.xmpp.authorization.External;
import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;

/**
 */
public class ServerResponsesTestCase extends TestCase {

    public void testFeaturesForAuthentication() throws ParsingException {
        StringStreamParser parser = new StringStreamParser(
                "<features>" +
                  "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" +
                    "<mechanism>EXTERNAL</mechanism>" +
                    "<mechanism>PLAIN</mechanism>" +
                    "<mechanism>ANONYMOUS</mechanism>" +
                  "</mechanisms>" +
                "</features>");

        Stanza stanza = parser.getNextStanza();


        List<SASLMechanism> mechanismList = new ArrayList<SASLMechanism>();
        mechanismList.add(new External());
        mechanismList.add(new Plain());
        mechanismList.add(new Anonymous());
        // add others
        assertEquals("stanzas are identical", stanza.toString(), new ServerResponses().getFeaturesForAuthentication(mechanismList).toString());
    }
}
