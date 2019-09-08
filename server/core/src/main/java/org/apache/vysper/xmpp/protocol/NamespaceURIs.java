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

/**
 * common namespace URIs as defined by RFC3920 and RFC3921
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class NamespaceURIs {

    public static final String XML = "http://www.w3.org/XML/1998/namespace";

    public static final String HTTP_ETHERX_JABBER_ORG_STREAMS = "http://etherx.jabber.org/streams";

    public static final String URN_IETF_PARAMS_XML_NS_XMPP_BIND = "urn:ietf:params:xml:ns:xmpp-bind";

    public static final String URN_IETF_PARAMS_XML_NS_XMPP_TLS = "urn:ietf:params:xml:ns:xmpp-tls";

    public static final String URN_IETF_PARAMS_XML_NS_XMPP_SASL = "urn:ietf:params:xml:ns:xmpp-sasl";

    public static final String URN_IETF_PARAMS_XML_NS_XMPP_STREAMS = "urn:ietf:params:xml:ns:xmpp-streams";

    public static final String URN_IETF_PARAMS_XML_NS_XMPP_STANZAS = "urn:ietf:params:xml:ns:xmpp-stanzas";

    public static final String URN_IETF_PARAMS_XML_NS_XMPP_SESSION = "urn:ietf:params:xml:ns:xmpp-session";

    public static final String JABBER_CLIENT = "jabber:client";

    public static final String JABBER_SERVER = "jabber:server";

    public static final String JABBER_SERVER_DIALBACK = "jabber:server:dialback";

    public static final String JABBER_IQ_REGISTER = "jabber:iq:register";

    public static final String JABBER_IQ_ROSTER = "jabber:iq:roster";

    // compatibility namespaces
    public static final String JABBER_IQ_AUTH_COMPATIBILITY = "jabber:iq:auth";

    // extension namespaces
    public static final String JABBER_IQ_VERSION = "jabber:iq:version";

    public static final String JABBER_IQ_TIME = "jabber:iq:time";

    public static final String JABBER_X_DATA = "jabber:x:data";

    public static final String PRIVATE_DATA = "jabber:iq:private";

    public static final String URN_XMPP_TIME = "urn:xmpp:time";

    public static final String URN_XMPP_PING = "urn:xmpp:ping";

    public static final String URN_XMPP_DELAY = "urn:xmpp:delay";
    
    public static final String URN_XMPP_XBOSH = "urn:xmpp:xbosh";

    public static final String URN_XMPP_FEATURES_DIALBACK = "urn:xmpp:features:dialback";

    public static final String VCARD_TEMP = "vcard-temp";

    public static final String JABBER_ORG_FEATURES_IQ_REGISTER = "http://jabber.org/features/iq-register";

    public static final String XEP0030_SERVICE_DISCOVERY_ITEMS = "http://jabber.org/protocol/disco#items";

    public static final String XEP0030_SERVICE_DISCOVERY_INFO = "http://jabber.org/protocol/disco#info";

    public static final String XEP0045_MUC = "http://jabber.org/protocol/muc";

    public static final String XEP0045_MUC_ADMIN = "http://jabber.org/protocol/muc#admin";

    public static final String XEP0045_MUC_OWNER = "http://jabber.org/protocol/muc#owner";

    public static final String XEP0045_MUC_REQUEST = "http://jabber.org/protocol/muc#request";

    public static final String XEP0045_MUC_UNIQUE = "http://jabber.org/protocol/muc#unique";

    public static final String XEP0045_MUC_USER = "http://jabber.org/protocol/muc#user";

    public static final String XEP0050_ADHOC_COMMANDS = "http://jabber.org/protocol/commands";

    public static final String XEP0060_PUBSUB = "http://jabber.org/protocol/pubsub";

    public static final String XEP0060_PUBSUB_EVENT = "http://jabber.org/protocol/pubsub#event";

    public static final String XEP0060_PUBSUB_OWNER = "http://jabber.org/protocol/pubsub#owner";

    public static final String XEP0060_PUBSUB_ERRORS = "http://jabber.org/protocol/pubsub#errors";

    public static final String XEP0065_SOCKS5_BYTESTREAMS = "http://jabber.org/protocol/bytestreams";

    public static final String XEP0124_BOSH = "http://jabber.org/protocol/httpbind";
    
    public static final String XEP0133_SERVICE_ADMIN = "http://jabber.org/protocol/admin";
    
    public static final String XEP0059_RESULT_SET_MANAGEMENT = "http://jabber.org/protocol/rsm";
    
    public static final String XEP0297_STANZA_FORWARDING = "urn:xmpp:forward:0";

    public static final String XEP0359_STANZA_IDS = "urn:xmpp:sid:0";
    
    public static final String XEP0334_MESSAGE_PROCESSING_HINTS = "urn:xmpp:hints";
}
