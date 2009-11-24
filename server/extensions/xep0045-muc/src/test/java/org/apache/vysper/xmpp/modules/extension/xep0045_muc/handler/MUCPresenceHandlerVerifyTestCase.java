package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import junit.framework.TestCase;

import org.apache.vysper.TestUtil;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public class MUCPresenceHandlerVerifyTestCase extends TestCase {

    private static final Entity FROM = TestUtil.parseUnchecked("user@vysper.org");
    private static final Entity TO = TestUtil.parseUnchecked("room@chat.vysper.org");

    private MUCPresenceHandler presenceHandler;
    
    @Override
    protected void setUp() throws Exception {
        Conference conference = new Conference("foo");
        presenceHandler = new MUCPresenceHandler(conference);
    }

    public void testVerifyNonPresence() {
        StanzaBuilder builder = StanzaBuilder.createMessageStanza(FROM, TO, "en", "foo");
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC);
        builder.endInnerElement();
        
        assertFalse(presenceHandler.verify(builder.build()));
    }


    public void testVerifyWithMUCNamespace() {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(FROM, TO, null, null, null, null);
        builder.startInnerElement("x", NamespaceURIs.XEP0045_MUC);
        builder.endInnerElement();
        
        assertTrue(presenceHandler.verify(builder.build()));
    }

    public void testVerifyWithNonMUCNamespace() {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(FROM, TO, null, null, null, null);
        builder.startInnerElement("x", "foo");
        builder.endInnerElement();
        
        assertFalse(presenceHandler.verify(builder.build()));
    }

    
    public void testVerifyWithoutMUCNamespace() {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(FROM, TO, null, null, null, null);
        
        assertTrue(presenceHandler.verify(builder.build()));
    }
    
    
    public void testVerifyWithoutMUCNamespaceInnerElement() {
        StanzaBuilder builder = StanzaBuilder.createPresenceStanza(FROM, TO, null, null, null, null);
        builder.startInnerElement("foo").endInnerElement();
        
        assertTrue(presenceHandler.verify(builder.build()));
    }
}
