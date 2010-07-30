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
package org.apache.vysper.xmpp.extension.xep0124.inttests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.ClientProtocolException;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Integration tests with Apache HttpComponents Client
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshIntegrationTest extends IntegrationTestTemplate {

    @Test
    public void test() throws ClientProtocolException, IOException, IllegalStateException, SAXException {
        // initial request
        Stanza boshResponse = sendRequest("<body rid='100' xmlns='http://jabber.org/protocol/httpbind' to='vysper.org' xml:lang='en' wait='60' hold='1' ver='1.9' xmpp:version='1.0' xmlns:xmpp='urn:xmpp:xbosh'/>");
        String sid = boshResponse.getAttributeValue("sid");
        assertNotNull(sid);
        assertNotNull(boshResponse.getAttributeValue("requests"));
        assertNotNull(boshResponse.getAttributeValue("inactivity"));
        assertNotNull(boshResponse.getAttributeValue("hold"));
        assertNotNull(boshResponse.getAttributeValue("wait"));
        assertNotNull(boshResponse.getAttributeValue("polling"));
        assertEquals("vysper.org", boshResponse.getAttributeValue("from"));
        assertEquals("1.9", boshResponse.getAttributeValue("ver"));
        assertEquals(1, boshResponse.getInnerElements().size());
        assertEquals("features", boshResponse.getInnerElements().get(0).getName());
        assertEquals(NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS, boshResponse.getInnerElements().get(0)
                .getNamespaceURI());
        assertFalse(boshResponse.getInnerElements().get(0).getInnerElements().isEmpty());
        XMLElement mechanisms = null;
        for (XMLElement element : boshResponse.getInnerElements().get(0).getInnerElements()) {
            if (element.getName().equals("mechanisms")
                    && element.getNamespaceURI().equals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)) {
                mechanisms = element;
                break;
            }
        }
        assertNotNull(mechanisms);
        boolean isPlain = false;
        for (XMLElement element : mechanisms.getInnerElements()) {
            assertEquals("mechanism", element.getName());
            if (element.getInnerText().getText().equals("PLAIN")) {
                isPlain = true;
            }
        }
        assertTrue("Only plain auth supported by test", isPlain);

        // SASL request
        String auth = getPlainAuth("user1", "password1");
        boshResponse = sendRequest("<body rid='101' xmlns='http://jabber.org/protocol/httpbind' sid='" + sid
                + "'><auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>" + auth + "</auth></body>");
        assertEquals(1, boshResponse.getInnerElements().size());
        assertEquals("success", boshResponse.getInnerElements().get(0).getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL, boshResponse.getInnerElements().get(0)
                .getNamespaceURI());

        // connection restart
        boshResponse = sendRequest("<body rid='102' xmlns='http://jabber.org/protocol/httpbind' sid='" + sid
                + "' to='vysper.org' xml:lang='en' xmpp:restart='true' xmlns:xmpp='urn:xmpp:xbosh'/>");
        assertEquals(1, boshResponse.getInnerElements().size());
        assertEquals("features", boshResponse.getInnerElements().get(0).getName());
        assertEquals(NamespaceURIs.HTTP_ETHERX_JABBER_ORG_STREAMS, boshResponse.getInnerElements().get(0)
                .getNamespaceURI());
        assertFalse(boshResponse.getInnerElements().get(0).getInnerElements().isEmpty());
        boolean isBind = false;
        for (XMLElement element : boshResponse.getInnerElements().get(0).getInnerElements()) {
            if (element.getName().equals("bind")
                    && element.getNamespaceURI().equals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND)) {
                isBind = true;
                break;
            }
        }

        assertTrue("Cannot test further because the 'bind' feature is not present after connection restart.", isBind);

        // resource binding
        // note that Vysper will ignore the resource from the client and will generate its own resource (this is allowed by the XMPP specification)
        boshResponse = sendRequest("<body rid='103' xmlns='http://jabber.org/protocol/httpbind' sid='"
                + sid
                + "'><iq type='set' id='200' xmlns='jabber:client'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'><resource>"
                + "HttpClient</resource></bind></iq></body>");
        assertEquals(1, boshResponse.getInnerElements().size());
        XMLElement iq = boshResponse.getInnerElements().get(0);
        assertEquals("iq", iq.getName());
        assertEquals(NamespaceURIs.JABBER_CLIENT, iq.getNamespaceURI());
        assertEquals("result", iq.getAttributeValue("type"));
        assertEquals("200", iq.getAttributeValue("id"));
        assertEquals(1, iq.getInnerElements().size());
        XMLElement bind = iq.getInnerElements().get(0);
        assertEquals("bind", bind.getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_BIND, bind.getNamespaceURI());
        assertEquals(1, bind.getInnerElements().size());
        assertEquals("jid", bind.getInnerElements().get(0).getName());
        String jid = bind.getInnerElements().get(0).getInnerText().getText();
        assertNotNull(jid);
        assertTrue(jid.matches("user1@vysper.org/.+"));
        
        System.out.println("JID " + jid + " is connected to the Vysper server");
        
     // session termination
        boshResponse = sendRequest("<body rid='104' xmlns='http://jabber.org/protocol/httpbind' sid='" + sid + "' type='terminate'><presence type='unavailable' from='" + jid + "' xmlns='jabber:client'/></body>");
        assertTrue(boshResponse.getInnerElements().isEmpty());
        assertEquals("terminate", boshResponse.getAttributeValue("type"));
    }

    private String getPlainAuth(String user, String pass) {
        List<Byte> list = new ArrayList<Byte>();
        list.add((byte) 0);
        addAll(list, user);
        list.add((byte) 0);
        addAll(list, pass);
        byte[] array = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return new String(Base64.encodeBase64(array));
    }

    private void addAll(List<Byte> list, String s) {
        for (byte b : s.getBytes()) {
            list.add(b);
        }
    }

}
