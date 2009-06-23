package org.apache.vysper.xmpp.modules.extension.xep0199_xmppping;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class XmppPingIQHandlerTestCase extends TestCase {
    
    private static final String IQ_ID = "id1";

    private TestSessionContext sessionContext;

    protected Entity client;
    protected Entity boundClient;
    protected Entity server;
    protected XmppPingIQHandler handler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        client = EntityImpl.parse("tester@vysper.org");
        
        sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
        sessionContext.setInitiatingEntity(client);
        
        boundClient = new EntityImpl(client, sessionContext.bindResource());
        server = sessionContext.getServerJID();
        
        handler = new XmppPingIQHandler();
    }

    public void testClientToServerPing() {
        // C: <iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='s2c1' type='get'>
        //      <ping xmlns='urn:xmpp:ping'/>
        //    </iq>
        //
        // S: <iq from='juliet@capulet.lit/balcony' to='capulet.lit' id='s2c1' type='result'/>
        
        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(boundClient, server, IQStanzaType.GET, IQ_ID);
        stanzaBuilder.startInnerElement("ping").addNamespaceAttribute(NamespaceURIs.URN_XMPP_PING).endInnerElement();

        Stanza requestStanza = stanzaBuilder.getFinalStanza();
        ResponseStanzaContainer resp = handler.execute(requestStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null);

        // we should always get a response
        assertTrue(resp.hasResponse());
        
        Stanza respStanza = resp.getResponseStanza();

        assertEquals("iq", respStanza.getName());
        assertEquals(boundClient, respStanza.getTo());
        assertEquals(server, respStanza.getFrom());
        assertEquals(IQ_ID, respStanza.getAttributeValue("id"));
        assertEquals("result", respStanza.getAttributeValue("type"));
    }
}
