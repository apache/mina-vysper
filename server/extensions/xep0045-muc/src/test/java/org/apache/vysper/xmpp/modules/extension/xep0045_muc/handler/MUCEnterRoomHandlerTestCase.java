package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.StanzaReceiverQueue;
import org.apache.vysper.xmpp.delivery.StanzaReceiverRelay;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.TestUtil;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 */
public class MUCEnterRoomHandlerTestCase extends TestCase {
    
    private TestSessionContext sessionContext;

    protected Entity room1Jid = TestUtil.parseUnchecked("room1@vysper.org");
    protected Entity room2Jid = TestUtil.parseUnchecked("room2@vysper.org");

    protected Entity room1JidWithNick = TestUtil.parseUnchecked("room1@vysper.org/nick");
    protected Entity room2JidWithNick = TestUtil.parseUnchecked("room2@vysper.org/nick");
    
    protected Entity occupant1Jid = TestUtil.parseUnchecked("user1@vysper.org");
    protected Entity occupant2Jid = TestUtil.parseUnchecked("user2@vysper.org");
    protected MUCEnterRoomHandler handler;

    private Conference conference;

    private StanzaReceiverQueue occupant1Queue;

    private StanzaReceiverQueue occupant2Queue;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        sessionContext = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
        sessionContext.setInitiatingEntity(occupant1Jid);
        
        StanzaReceiverRelay stanzaRelay = (StanzaReceiverRelay) sessionContext.getServerRuntimeContext().getStanzaRelay();
        occupant1Queue = new StanzaReceiverQueue();
        occupant2Queue = new StanzaReceiverQueue();
        stanzaRelay.add(occupant1Jid, occupant1Queue);
        stanzaRelay.add(occupant2Jid, occupant2Queue);
        
        conference = new Conference("foo");
        conference.createRoom(room1Jid, "Room 1");
        
        handler = new MUCEnterRoomHandler(conference);
    }

    private Stanza enterRoom(Entity occupantJid, Entity roomJid) {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(occupantJid, roomJid, null, null, null, null);
        stanzaBuilder.startInnerElement("x").addNamespaceAttribute(NamespaceURIs.XEP0045_MUC).endInnerElement();

        Stanza presenceStanza = stanzaBuilder.getFinalStanza();
        ResponseStanzaContainer container = handler.execute(presenceStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null);
        if(container != null) {
            return container.getResponseStanza();
        } else {
            return null;
        }
    }
    
    public void testEnterExistingRoom() {
        Room room = conference.findRoom(room1Jid);
        assertEquals(0, room.getOccupants().size());

        enterRoom(occupant1Jid, room1JidWithNick);

        assertEquals(1, room.getOccupants().size());
        Occupant occupant = room.getOccupants().iterator().next();
        
        assertEquals(occupant1Jid, occupant.getJid());
        assertEquals("nick", occupant.getName());
    }

    public void testEnterNonExistingRoom() {
        Room room = conference.findRoom(room2Jid);
        assertNull(room);

        enterRoom(occupant1Jid, room2JidWithNick);

        room = conference.findRoom(room2Jid);
        assertNotNull(room);
        assertEquals(1, room.getOccupants().size());
        Occupant occupant = room.getOccupants().iterator().next();
        
        assertEquals(occupant1Jid, occupant.getJid());
        assertEquals("nick", occupant.getName());
    }
    
    public void testEnterWithoutNick() {
        // try entering without a nick
        PresenceStanza response = (PresenceStanza) enterRoom(occupant1Jid, room1Jid);

        assertNotNull(response);
        assertEquals("presence", response.getName());
        assertEquals(occupant1Jid, response.getTo());
        assertEquals(room1Jid, response.getFrom());
        assertEquals("error", response.getType());
        
        List<XMLElement> innerElements = response.getInnerElements();
        
        XMLElement xElement = innerElements.get(0);
        assertEquals("x", xElement.getName());
        assertEquals(NamespaceURIs.XEP0045_MUC, xElement.getNamespaceURI());

        XMLElement errorElement = innerElements.get(1);
        assertEquals("error", errorElement.getName());
        assertEquals("modify", errorElement.getAttributeValue("type"));
        
        XMLElement jidMalformedElement = errorElement.getFirstInnerElement();
        assertEquals("jid-malformed", jidMalformedElement.getName());
        assertEquals(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_STANZAS, jidMalformedElement.getNamespaceURI());
        
    }
    
    public void testEnterRoomWithRelays() throws Exception {


        // add one occupant to the room
        Room room = conference.findOrCreateRoom(room1Jid, "Room 1");
        room.addOccupant(occupant1Jid, "Some nick");
        
        // now, let user 2 enter room
        enterRoom(occupant2Jid, room1JidWithNick);

//        <presence
//        from='darkcave@chat.shakespeare.lit/firstwitch'
//        to='hag66@shakespeare.lit/pda'>
//      <x xmlns='http://jabber.org/protocol/muc#user'>
//        <item affiliation='owner' role='moderator'/>
//      </x>
//    </presence>

        // verify stanzas to existing occupants on the new user
        Stanza user2JoinedStanza = occupant1Queue.getNext();
        // should be from room + nick name
        assertEquals(room1Jid.getFullQualifiedName() + "/nick", user2JoinedStanza.getFrom().getFullQualifiedName());
        // should be to the existing user
        assertEquals(occupant1Jid, user2JoinedStanza.getTo());
        
        XMLElement xElement = user2JoinedStanza.getSingleInnerElementsNamed("x");
        assertEquals(NamespaceURIs.XEP0045_MUC_USER, xElement.getNamespaceURI());
        
        // since this room is non-anonymous, x must contain an item element with the users full JID
        XMLElement itemElement = xElement.getSingleInnerElementsNamed("item");
        assertEquals(occupant2Jid.getFullQualifiedName(), itemElement.getAttributeValue("jid"));
        assertEquals("none", itemElement.getAttributeValue("affiliation"));
        assertEquals("participant", itemElement.getAttributeValue("role"));


        // verify stanzas to the new user on all existing users, including himself with status=110 element
        // own presence must be sent last
        // assert the stanza from the already existing user
        Stanza stanza = occupant2Queue.getNext();
        assertNotNull(stanza);
        assertEquals(room1Jid.getFullQualifiedName() + "/Some nick", stanza.getFrom().getFullQualifiedName());
        assertEquals(occupant2Jid, stanza.getTo());

        // assert stanza from the joining user, must have extra status=110 element        
        stanza = occupant2Queue.getNext();
        assertNotNull(stanza);
        assertEquals(room1JidWithNick, stanza.getFrom());
        assertEquals(occupant2Jid, stanza.getTo());
        List<XMLElement> statusElements = stanza.getFirstInnerElement().getInnerElementsNamed("status");
        assertEquals(2, statusElements.size());
        assertEquals("100", statusElements.get(0).getAttributeValue("code"));
        assertEquals("110", statusElements.get(1).getAttributeValue("code"));
        
    }
}
