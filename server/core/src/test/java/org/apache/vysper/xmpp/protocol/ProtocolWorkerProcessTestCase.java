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

package org.apache.vysper.xmpp.protocol;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementVerifier;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;

import junit.framework.TestCase;

/**
 * test basic behavior of ProtocolWorker.processStanza()
 */
public class ProtocolWorkerProcessTestCase extends TestCase {
    private ProtocolWorker protocolWorker;

    private NamespaceHandlerDictionary namespaceHandlerDictionary;

    private DefaultServerRuntimeContext serverRuntimeContext;

    private TestSessionContext sessionContext;

    private static Entity serverEnitity = new EntityImpl(null, "test", null);

    private SessionStateHolder sessionStateHolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        namespaceHandlerDictionary = new NamespaceHandlerDictionary("testNSURI");
        StanzaReceiverRelay receiverRelay = new StanzaReceiverRelay();
        serverRuntimeContext = new DefaultServerRuntimeContext(serverEnitity, receiverRelay);
        protocolWorker = new ProtocolWorker(new SimpleStanzaHandlerExecutorFactory(receiverRelay));
        receiverRelay.setServerRuntimeContext(serverRuntimeContext);
        serverRuntimeContext.addDictionary(namespaceHandlerDictionary);
        sessionStateHolder = new SessionStateHolder();
        sessionContext = new TestSessionContext(serverRuntimeContext, sessionStateHolder,
                serverRuntimeContext.getStanzaRelay());
    }

    public void testProcessUnknownStanza() {

        sessionContext.setSessionState(SessionState.AUTHENTICATED);

        Stanza stanza = new StanzaBuilder("ProtocolWorkerProcessTestCase", "testNSURI").build();
        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);

        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        XMLElementVerifier verifier = recordedResponse.getVerifier();
        assertTrue("error", verifier.nameEquals("error"));
        assertTrue("unsupported stanza type",
                verifier.subElementPresent(StreamErrorCondition.UNSUPPORTED_STANZA_TYPE.value()));
    }

    public void testProcessStanzaNoResponse() {

        sessionContext.setSessionState(SessionState.AUTHENTICATED);

        CallTestStanzaHandler stanzaHandler = new CallTestStanzaHandler("ProtocolWorkerProcessTestCase");
        namespaceHandlerDictionary.register(stanzaHandler);

        Stanza stanza = new StanzaBuilder("ProtocolWorkerProcessTestCase", "testNSURI").build();

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);

        stanzaHandler.assertHandlerCalled();
        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        assertNull("no emmitter, no response", recordedResponse);
    }

    public void testProcessStanzaWithResponse() {

        sessionContext.setSessionState(SessionState.AUTHENTICATED);

        CallTestStanzaHandlerResponse stanzaHandler = new CallTestStanzaHandlerResponse(
                "ProtocolWorkerProcessTestCase");
        namespaceHandlerDictionary.register(stanzaHandler);

        Stanza stanza = new StanzaBuilder("ProtocolWorkerProcessTestCase", "testNSURI").build();
        Stanza responseStanza = new StanzaBuilder("response").build();

        stanzaHandler.setResponseStanza(responseStanza);

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);

        stanzaHandler.assertHandlerCalled();
        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        assertEquals("response handled", responseStanza, recordedResponse);

        stanzaHandler.setResponseStanza(null);
        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);
        assertNull("handler emmitted null as response", stanzaHandler.getUniqueResponseStanza());

    }

    public void testHandlerThrowProtocolException() {

        sessionContext.setSessionState(SessionState.AUTHENTICATED);

        CallTestStanzaHandler stanzaHandler = new CallTestStanzaHandler("ProtocolWorkerProcessTestCase");
        namespaceHandlerDictionary.register(stanzaHandler);
        stanzaHandler.setProtocolException(new ProtocolException("forced error"));

        Stanza stanza = new StanzaBuilder("ProtocolWorkerProcessTestCase", "testNSURI").build();

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);

        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        assertEquals("bad format", "error", recordedResponse.getName());
        assertTrue("closed", sessionContext.isClosed());
    }

    public void testProcessWrongStartStanza() {

        sessionContext.setSessionState(SessionState.INITIATED);

        CallTestStanzaHandler stanzaHandler = new CallTestStanzaHandler("ProtocolWorkerProcessTestCase");
        namespaceHandlerDictionary.register(stanzaHandler);

        Stanza stanza = new StanzaBuilder("ProtocolWorkerProcessTestCase", "testNSURI").build();

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanza,
                sessionStateHolder);

        try {
            stanzaHandler.assertHandlerCalled();
            fail("handler called");
        } catch (Exception e) {
            // not called, OK
        }
        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        assertEquals("open stream", "stream", recordedResponse.getName());
        XMLElementVerifier xmlElementVerifier = recordedResponse.getVerifier();
        assertTrue("error embedded", xmlElementVerifier.subElementPresent("error"));
        XMLElement error = (XMLElement) recordedResponse.getInnerFragments().get(0);
        assertEquals("bad format", "error", error.getName());
        assertTrue("closed", sessionContext.isClosed());
    }

    public void testDetectWrongFromAddress() throws XMLSemanticError {

        Entity server = sessionContext.getServerRuntimeContext().getServerEntity();
        sessionContext.setSessionState(SessionState.AUTHENTICATED);
        // the session is running for 'mark'
        sessionContext.setInitiatingEntity(new EntityImpl("mark", server.getDomain(), null));

        CallTestStanzaHandler stanzaHandler = new CallTestStanzaHandler("iq");
        namespaceHandlerDictionary.register(stanzaHandler);

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(null, null, IQStanzaType.GET, "test");
        // we set a different from user name 'ernest'!
        stanzaBuilder.addAttribute("from", new EntityImpl("ernest", server.getDomain(), null).getFullQualifiedName());
        stanzaBuilder.startInnerElement("query", "jabber:iq:roster").endInnerElement();

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);

        try {
            stanzaHandler.assertHandlerCalled();
            fail("handler called");
        } catch (Exception e) {
            // not called, OK
        }
        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        assertUnknownSenderError(recordedResponse);
    }

    public void testAllowProperFromResourceId() throws XMLSemanticError, BindException {

        Entity server = sessionContext.getServerRuntimeContext().getServerEntity();
        sessionContext.setSessionState(SessionState.AUTHENTICATED);
        // the session is running for 'charlotte'
        sessionContext.setInitiatingEntity(new EntityImpl("charlotte", server.getDomain(), null));

        CallTestStanzaHandler stanzaHandler = new CallTestStanzaHandler("iq", "testNSURI");
        namespaceHandlerDictionary.register(stanzaHandler);

        String onlyBoundResource = sessionContext.bindResource();

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(null, null, IQStanzaType.GET, "test");
        stanzaBuilder.addAttribute("from",
                new EntityImpl("charlotte", server.getDomain(), onlyBoundResource).getFullQualifiedName());
        stanzaBuilder.startInnerElement("query", "testNSURI").endInnerElement();

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);

        stanzaHandler.assertHandlerCalled();
    }

    public void testDetectWrongFromResourceId() throws XMLSemanticError, BindException {

        Entity server = sessionContext.getServerRuntimeContext().getServerEntity();
        sessionContext.setSessionState(SessionState.AUTHENTICATED);
        // the session is running for 'charlotte'
        sessionContext.setInitiatingEntity(new EntityImpl("charlotte", server.getDomain(), null));

        CallTestStanzaHandler stanzaHandler = new CallTestStanzaHandler("iq", "testNSURI");
        namespaceHandlerDictionary.register(stanzaHandler);

        String onlyBoundResource = sessionContext.bindResource();
        String arbitraryUnboundResource = "unboundResourceID";

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(null, null, IQStanzaType.GET, "test");

        stanzaBuilder.addAttribute("from",
                new EntityImpl("charlotte", server.getDomain(), arbitraryUnboundResource).getFullQualifiedName());
        stanzaBuilder.startInnerElement("query", "testNSURI").endInnerElement();

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);

        try {
            stanzaHandler.assertHandlerCalled();
            fail("handler called");
        } catch (Exception e) {
            // not called, OK
        }
        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        assertUnknownSenderError(recordedResponse);
    }

    public void testAllowBareFromEntityOnlyOnSingleBoundResource() throws XMLSemanticError, BindException {
        // from="client@vysper.org" is only allowed when a single resource is bound
        // if there is more than one resource bound in the same session, from must come
        // fully qualified, e.g. from="client@vysper.org/resourceId"

        Entity server = sessionContext.getServerRuntimeContext().getServerEntity();
        sessionContext.setSessionState(SessionState.AUTHENTICATED);
        // the session is running for 'lea'
        sessionContext.setInitiatingEntity(new EntityImpl("lea", server.getDomain(), null));

        CallTestStanzaHandler stanzaHandler = new CallTestStanzaHandler("iq", "testNSURI");
        namespaceHandlerDictionary.register(stanzaHandler);

        String firstBoundResource = sessionContext.bindResource();
        String secondBoundResource = sessionContext.bindResource();

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(null, null, IQStanzaType.GET, "test");
        stanzaBuilder.addAttribute("from", new EntityImpl("lea", server.getDomain(), null).getFullQualifiedName());
        stanzaBuilder.startInnerElement("query", "testNSURI").endInnerElement();

        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);
        Stanza recordedResponse = sessionContext.getNextRecordedResponse();
        assertUnknownSenderError(recordedResponse); // not allowed, bare id without resource and two resources bound
        sessionContext.reset();

        // unbind second resource, leaving only one
        boolean noResourceRemains = sessionContext.getServerRuntimeContext().getResourceRegistry()
                .unbindResource(secondBoundResource);
        assertFalse(noResourceRemains);

        // bare id allowed, only one resource is bound
        stanzaBuilder = StanzaBuilder.createIQStanza(null, null, IQStanzaType.GET, "test");
        stanzaBuilder.addAttribute("from", new EntityImpl("lea", server.getDomain(), null).getFullQualifiedName());
        stanzaBuilder.startInnerElement("query", "testNSURI").endInnerElement();
        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);
        stanzaHandler.assertHandlerCalled();
        sessionContext.reset();

        // second resource is now invalid and cannot be used anymore in a full qualified
        // entity
        stanzaBuilder = StanzaBuilder.createIQStanza(null, null, IQStanzaType.GET, "test");
        stanzaBuilder.addAttribute("from",
                new EntityImpl("lea", server.getDomain(), secondBoundResource).getFullQualifiedName());
        stanzaBuilder.startInnerElement("query", "testNSURI").endInnerElement();
        protocolWorker.processStanza(sessionContext.getServerRuntimeContext(), sessionContext, stanzaBuilder.build(),
                sessionStateHolder);
        recordedResponse = sessionContext.getNextRecordedResponse();
        assertUnknownSenderError(recordedResponse);

    }

    private void assertUnknownSenderError(Stanza recordedResponse) throws XMLSemanticError {
        XMLElementVerifier verifier = recordedResponse.getVerifier();
        assertEquals("iq stanza error", "iq", recordedResponse.getName());
        IQStanza iqStanza = (IQStanza) XMPPCoreStanza.getWrapper(recordedResponse);
        assertEquals("error", iqStanza.getType());
        assertTrue("error embedded", verifier.subElementPresent("error"));
        XMLElement errorInner = recordedResponse.getSingleInnerElementsNamed("error");
        assertEquals("modify", errorInner.getAttributeValue("type"));
        XMLElementVerifier errorVerifier = errorInner.getVerifier();
        errorVerifier.subElementPresent("unknown-sender");
    }

}
