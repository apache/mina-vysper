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
package org.apache.vysper.xmpp.modules.extension.xep0049_privatedata;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.decoder.DocumentContentHandler;
import org.apache.vysper.xml.decoder.XMLElementListener;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.sax.NonBlockingXMLReader;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.xml.sax.SAXException;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0049", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class PrivateDataIQHandler extends DefaultIQHandler {

    protected PrivateDataPersistenceManager persistenceManager;

    public void setPersistenceManager(PrivateDataPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query") && verifyInnerNamespace(stanza, NamespaceURIs.PRIVATE_DATA);
    }

    @Override
    protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        // Get From
        Entity to = stanza.getTo();
        Entity from = stanza.getFrom();
        if (from == null) {
            from = sessionContext.getInitiatingEntity();
        }

        // Not null, and not addressed to itself
        if (to != null && !to.getBareJID().equals(sessionContext.getInitiatingEntity().getBareJID())) {
            return ServerErrorResponses.getStanzaError(StanzaErrorCondition.FORBIDDEN, stanza,
                    StanzaErrorType.CANCEL, 403, "Private data only modifiable by the owner", null, null);
        }

        XMLElement queryElement = stanza.getFirstInnerElement();

        // Example 4: http://xmpp.org/extensions/xep-0049.html
        // Query element must have a child element with a non-null namespace
        if (queryElement.getInnerElements().size() != 1) {
            return ServerErrorResponses.getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE, stanza,
                    StanzaErrorType.MODIFY, "query's child element is missing", null, null);
        }
        XMLElement x = queryElement.getFirstInnerElement();
        String ns = x.getNamespaceURI();

        // No persistance Manager
        if (persistenceManager == null) {
            return ServerErrorResponses.getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR,
                    stanza, StanzaErrorType.WAIT, "internal storage inaccessible", null, null);
        }

        String queryKey = getKey(x);
        String queryContent = new Renderer(x).getComplete();
        boolean success = persistenceManager.setPrivateData(from, queryKey, queryContent);

        if (success) {
            return StanzaBuilder.createIQStanza(null, from, IQStanzaType.RESULT, stanza.getID()).build();
        } else {
            return StanzaBuilder.createIQStanza(null, from, IQStanzaType.ERROR, stanza.getID()).build();
        }
    }

    @Override
    protected Stanza handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        Entity to = stanza.getTo();
        Entity from = stanza.getFrom();
        if (from == null) {
            from = sessionContext.getInitiatingEntity();
        }

        // Not null, and not addressed to itself
        if (to != null && !to.getBareJID().equals(sessionContext.getInitiatingEntity().getBareJID())) {
            return ServerErrorResponses.getStanzaError(StanzaErrorCondition.FORBIDDEN, stanza,
                    StanzaErrorType.CANCEL, 403, "can only view your data", null, null);
        }

        XMLElement queryElement = stanza.getFirstInnerElement();
        XMLElement x = queryElement.getFirstInnerElement();
        if (x == null) {
            return ServerErrorResponses.getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE, stanza,
                    StanzaErrorType.MODIFY, "query's child element missing", null, null);
        }

        // No persistancy Manager
        if (persistenceManager == null) {
            return buildInteralStorageError(stanza);
        }

        String queryKey = getKey(x);
        String privateDataXML = persistenceManager.getPrivateData(from, queryKey);

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(),
                IQStanzaType.RESULT, stanza.getID());

        stanzaBuilder.startInnerElement("query", NamespaceURIs.PRIVATE_DATA);
        if (privateDataXML != null) {
            try {
                XMLElement elm = parseDocument(privateDataXML);
                stanzaBuilder.addPreparedElement(elm);
            } catch (Exception e) {
                return buildInteralStorageError(stanza);
            }
        } else {
            stanzaBuilder.addPreparedElement(x);
        }
        return stanzaBuilder.build();
    }
    
    private Stanza buildInteralStorageError(XMPPCoreStanza stanza) {
        return ServerErrorResponses.getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR,
                stanza, StanzaErrorType.WAIT, "internal storage inaccessible", null, null);
    }

    private XMLElement parseDocument(String xml) throws IOException, SAXException {
        NonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();

        DocumentContentHandler contentHandler = new DocumentContentHandler();
        reader.setContentHandler(contentHandler);

        final List<XMLElement> documents = new ArrayList<XMLElement>();
        
        contentHandler.setListener(new XMLElementListener() {
            public void element(XMLElement element) {
                documents.add(element);
            }
        });

        IoBuffer buffer = IoBuffer.wrap(xml.getBytes(Charset.forName("UTF-8")));
        reader.parse(buffer, CharsetUtil.UTF8_DECODER);

        return documents.get(0);
    }
    
    /**
     * Create a property name that is unique for this query. eg this XMLElement:
     * <storage xmlns="storage:bookmarks"> is converted into this string:
     * storage-storage-bookmarks
     */
    private String getKey(XMLElement x) {
        StringBuilder queryKey = new StringBuilder();
        queryKey.append(x.getName());
        queryKey.append("-");
        queryKey.append(x.getNamespaceURI());

        // Some characters are not valid for property names
        for (int i = 0; i < queryKey.length(); i++) {
            char c = queryKey.charAt(i);
            if (c == ' ' || c == ':') {
                queryKey.setCharAt(i, '-');
            }
        }
        return queryKey.toString();
    }

}
