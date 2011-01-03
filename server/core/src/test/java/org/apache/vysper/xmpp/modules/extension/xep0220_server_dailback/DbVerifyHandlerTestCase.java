package org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback;
import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.mockito.Mockito;


public class DbVerifyHandlerTestCase extends TestCase {

    private static final Entity FROM = EntityImpl.parseUnchecked("from.org");
    private static final Entity TO = EntityImpl.parseUnchecked("to.org");
    private static final String ID = "D60000229F";

    private DbVerifyHandler handler = new DbVerifyHandler();
    
    public void testVerify() {
        Stanza correct = new StanzaBuilder("verify", NamespaceURIs.JABBER_SERVER_DIALBACK).build();
        Stanza invalidNamespace = new StanzaBuilder("verify", "dummy").build();
        Stanza invalidName = new StanzaBuilder("dummy", NamespaceURIs.JABBER_SERVER_DIALBACK).build();
        
        Assert.assertTrue(handler.verify(correct));
        Assert.assertFalse(handler.verify(invalidNamespace));
        Assert.assertFalse(handler.verify(invalidName));
    }
    
    public void testExecuteValidVerification() {
        String token = new DialbackIdGenerator().generate(FROM, TO, ID);
        assertExecuteVerification(token, "valid");
    }

    public void testExecuteInvalidVerification() {
        assertExecuteVerification("12345", "invalid");
    }

    private void assertExecuteVerification(String token, String expectedType) {
        ServerRuntimeContext serverRuntimeContext = Mockito.mock(ServerRuntimeContext.class);
        Mockito.when(serverRuntimeContext.getServerEnitity()).thenReturn(TO);
        
        Stanza stanza = new StanzaBuilder("verify", NamespaceURIs.JABBER_SERVER_DIALBACK)
        .addAttribute("from", FROM.getFullQualifiedName())
        .addAttribute("to", TO.getFullQualifiedName())
        .addAttribute("id", ID)
        .addText(token)
        .build();
        
        Stanza response = handler.execute(stanza, serverRuntimeContext, false, null, null).getResponseStanza();
        
        Assert.assertNotNull(response);
        Assert.assertEquals(TO, response.getFrom());
        Assert.assertEquals(FROM, response.getTo());
        Assert.assertEquals(ID, response.getAttributeValue("id"));
        Assert.assertEquals(expectedType, response.getAttributeValue("type"));
    }
    
    

}
