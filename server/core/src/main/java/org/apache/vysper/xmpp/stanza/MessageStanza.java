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

package org.apache.vysper.xmpp.stanza;

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.FINISHED;

import java.util.Map;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;

/**
 * message stanza (push)
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "RFC3921bis-08", section = "5", status = FINISHED, coverage = COMPLETE)
public class MessageStanza extends XMPPCoreStanza {

    public static final String NAME = "message";

    public static boolean isOfType(Stanza stanza) {
        return isOfType(stanza, NAME);
    }

    public MessageStanza(Stanza stanza) {
        super(stanza);
        if (!MessageStanza.isOfType(stanza))
            throw new IllegalArgumentException("only 'message' stanza is allowed here");
    }

    @Override
    public String getName() {
        return NAME;
    }

    public MessageStanzaType getMessageType() {
        String type = getType();
        return MessageStanzaType.valueOfWithDefault(type);
    }

    /**
     *
     * @param lang
     * @return
     * @throws XMLSemanticError - if language attribtues are not unqiue RFC3921/2.1.2.1
     */
    public String getSubject(String lang) throws XMLSemanticError {
        XMLElement element = getSubjects().get(lang);
        if (element == null)
            return null;
        return element.getSingleInnerText().getText();
    }

    /**
     * @return all subject elements, keyed by their lang attribute
     * @throws XMLSemanticError
     */
    public Map<String, XMLElement> getSubjects() throws XMLSemanticError {
        return getInnerElementsByXMLLangNamed("subject");
    }

    /**
     *
     * @param lang
     * @return
     * @throws XMLSemanticError - if language attributes are not unique, RFC3921/2.1.2.2
     */
    public String getBody(String lang) throws XMLSemanticError {
        XMLElement element = getBodies().get(lang);
        if (element == null)
            return null;
        return element.getSingleInnerText().getText();
    }

    /**
     * @return all body elements, keyed by their lang attribute
     * @throws XMLSemanticError
     */
    public Map<String, XMLElement> getBodies() throws XMLSemanticError {
        return getInnerElementsByXMLLangNamed("body");
    }

    /**
     *
     * @return thread identifier, or NULL, if not given
     * @throws XMLSemanticError - if thread element is not unique, or no unqiue inner text
     * is given, RFC3921 2.1.2.3
     */
    public String getThread() throws XMLSemanticError {
        XMLElement element = getSingleInnerElementsNamed("thread");
        if (element == null)
            return null; // thread is optional, see RFC3921/2.1.2.3
        return element.getSingleInnerText().getText();
    }

}
