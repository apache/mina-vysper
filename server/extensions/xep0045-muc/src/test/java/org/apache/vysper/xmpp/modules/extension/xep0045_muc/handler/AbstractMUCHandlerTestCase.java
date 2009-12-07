package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.List;

import junit.framework.TestCase;

import org.apache.vysper.TestUtil;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaReceiverQueue;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;

/**
 */
public abstract class AbstractMUCHandlerTestCase extends TestCase {
    
    protected TestSessionContext sessionContext;

    protected static final String SERVERDOMAIN = "test";
    protected static final String SUBDOMAIN = "chat";
    protected static final String FULLDOMAIN = SUBDOMAIN + "." + SERVERDOMAIN;
    
    protected static final Entity MODULE_JID = TestUtil.parseUnchecked(FULLDOMAIN);

    protected static final Entity ROOM1_JID = TestUtil.parseUnchecked("room1@" + FULLDOMAIN);
    protected static final Entity ROOM2_JID = TestUtil.parseUnchecked("room2@" + FULLDOMAIN);

    protected static final Entity ROOM1_JID_WITH_NICK = TestUtil.parseUnchecked("room1@" + FULLDOMAIN + "/nick");
    protected static final Entity ROOM2_JID_WITH_NICK = TestUtil.parseUnchecked("room2@" + FULLDOMAIN + "/nick");
    
    protected static final Entity OCCUPANT1_JID = TestUtil.parseUnchecked("user1@" + SERVERDOMAIN);
    protected static final Entity OCCUPANT2_JID = TestUtil.parseUnchecked("user2@" + SERVERDOMAIN);
    protected StanzaHandler handler;

    protected Conference conference = new Conference("foo");

    protected StanzaReceiverQueue occupant1Queue = new StanzaReceiverQueue();

    protected StanzaReceiverQueue occupant2Queue = new StanzaReceiverQueue();
    
    @Override
    protected void setUp() throws Exception {
        sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
        sessionContext.setInitiatingEntity(OCCUPANT1_JID);
        
        StanzaReceiverRelay stanzaRelay = (StanzaReceiverRelay) sessionContext.getServerRuntimeContext().getStanzaRelay();
        stanzaRelay.add(OCCUPANT1_JID, occupant1Queue);
        stanzaRelay.add(OCCUPANT2_JID, occupant2Queue);
        
        conference.createRoom(ROOM1_JID, "Room 1");
        
        handler = createHandler();
    }
    
    protected abstract StanzaHandler createHandler();
    
    
    protected void assertErrorStanza(Stanza response, String stanzaName, Entity from, Entity to, 
            String type, String errorName, XMLElement... expectedInnerElements) {
        assertNotNull(response);
        assertEquals(stanzaName, response.getName());
        assertEquals(to, response.getTo());
        assertEquals(from, response.getFrom());
        assertEquals("error", response.getAttributeValue("type"));
        
        List<XMLElement> innerElements = response.getInnerElements();

        int index = 0;
        if(expectedInnerElements != null) {
            for(XMLElement expectedInnerElement : expectedInnerElements) {
                assertEquals(expectedInnerElement, innerElements.get(index));
                index++;
            }
        }
        
        // error element must always be present
        XMLElement errorElement = innerElements.get(index);
        assertEquals("error", errorElement.getName());
        assertEquals(type, errorElement.getAttributeValue("type"));
        
        XMLElement jidMalformedElement = errorElement.getFirstInnerElement();
        assertEquals(errorName, jidMalformedElement.getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, jidMalformedElement.getNamespaceURI());
    }
}
