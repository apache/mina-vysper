package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.History;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.NamespaceAttribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;

/**
 */
public class MUCPresenceHandlerEnterRoomTestCase extends AbstractMUCHandlerTestCase {

    private Stanza enterRoom(Entity occupantJid, Entity roomJid) throws ProtocolException {
        return enterRoom(occupantJid, roomJid, null, null, false);
    }
    
    private Stanza enterRoom(Entity occupantJid, Entity roomJid, String password, History history, boolean oldProtocol) throws ProtocolException {
        SessionContext userSessionContext;
        if(occupantJid.equals(OCCUPANT1_JID)) {
            userSessionContext = sessionContext;
        } else {
            userSessionContext = sessionContext2;
        }
        
        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(occupantJid, roomJid, null, null, null, null);
        if(!oldProtocol) {
            stanzaBuilder.startInnerElement("x").addNamespaceAttribute(NamespaceURIs.XEP0045_MUC);
            if(password != null) {
                stanzaBuilder.startInnerElement("password").addText(password).endInnerElement();
            }
            if(history != null) {
                stanzaBuilder.addPreparedElement(history);
            }
            
            stanzaBuilder.endInnerElement();
        }
        Stanza presenceStanza = stanzaBuilder.getFinalStanza();
        ResponseStanzaContainer container = handler.execute(presenceStanza, userSessionContext.getServerRuntimeContext(), true, userSessionContext, null);
        if(container != null) {
            return container.getResponseStanza();
        } else {
            return null;
        }
    }

    protected TestSessionContext sessionContext2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        sessionContext2 = TestSessionContext.createWithStanzaReceiverRelayAuthenticated();
        sessionContext2.setInitiatingEntity(OCCUPANT2_JID);
    }

    @Override
    protected StanzaHandler createHandler() {
        return new MUCPresenceHandler(conference);
    }
    
    public void testEnterExistingRoom() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        assertEquals(0, room.getOccupants().size());

        enterRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK);

        assertEquals(1, room.getOccupants().size());
        Occupant occupant = room.getOccupants().iterator().next();
        
        assertEquals(OCCUPANT1_JID, occupant.getJid());
        assertEquals("nick", occupant.getName());
    }

    public void testEnterWithGroupchatProtocol() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        assertEquals(0, room.getOccupants().size());

        // enter using the old groupchat protocol
        enterRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK, null, null, true);

        assertEquals(1, room.getOccupants().size());
        Occupant occupant = room.getOccupants().iterator().next();
        
        assertEquals(OCCUPANT1_JID, occupant.getJid());
        assertEquals("nick", occupant.getName());
    }

    public void testEnterAsAdmin() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Admin);

        enterRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK);

        Occupant occupant = room.getOccupants().iterator().next();
        
        assertEquals(Affiliation.Admin, occupant.getAffiliation());
    }

    public void testEnterAsOutcast() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.getAffiliations().add(OCCUPANT1_JID, Affiliation.Outcast);

        Stanza error = enterRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK);
        assertPresenceErrorStanza(error, ROOM1_JID, OCCUPANT1_JID, "auth", "forbidden");

        assertEquals(0, room.getOccupants().size());
    }

    
    public void testEnterRoomWithDuplicateNick() throws Exception {
        assertNull(enterRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK));
        Stanza error = enterRoom(OCCUPANT2_JID, ROOM1_JID_WITH_NICK);
        
        assertNotNull(error);
    }

    public void testEnterNonExistingRoom() throws Exception {
        Room room = conference.findRoom(ROOM2_JID);
        assertNull(room);

        enterRoom(OCCUPANT1_JID, ROOM2_JID_WITH_NICK);

        room = conference.findRoom(ROOM2_JID);
        assertNotNull(room);
        assertEquals(1, room.getOccupants().size());
        Occupant occupant = room.getOccupants().iterator().next();
        
        assertEquals(OCCUPANT1_JID, occupant.getJid());
        assertEquals("nick", occupant.getName());
    }
    
    public void testEnterWithoutNick() throws Exception {
        // try entering without a nick
        Stanza response = enterRoom(OCCUPANT1_JID, ROOM1_JID);

        assertPresenceErrorStanza(response, ROOM1_JID, OCCUPANT1_JID, "modify", "jid-malformed");
    }
    
    public void testEnterWithPassword() throws Exception {
        Room room = conference.createRoom(ROOM2_JID, "Room 1", RoomType.PasswordProtected);
        room.setPassword("secret");

        // no error should be returned
        assertNull(enterRoom(OCCUPANT1_JID, ROOM2_JID_WITH_NICK, "secret", null, false));
        assertEquals(1, room.getOccupants().size());
    }
    
    public void testEnterWithoutPassword() throws Exception {
        Room room = conference.createRoom(ROOM2_JID, "Room 1", RoomType.PasswordProtected);
        room.setPassword("secret");

        // try entering without a password
        Stanza response = enterRoom(OCCUPANT1_JID, ROOM2_JID_WITH_NICK);
        
        assertPresenceErrorStanza(response, ROOM2_JID, OCCUPANT1_JID, "auth", "not-authorized");
    }

    private void assertPresenceErrorStanza(Stanza response, Entity from, Entity to,
            String type, String errorName) {
        Attribute xmlns = new NamespaceAttribute(NamespaceURIs.XEP0045_MUC);
        XMLElement xElement = new XMLElement("x", null, Arrays.asList(xmlns), (XMLFragment[])null);
        assertErrorStanza(response, "presence", from, to, type, errorName, Arrays.asList(xElement));
    }

    
    public void testEnterRoomWithRelays() throws Exception {


        // add one occupant to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT2_JID, "Some nick");
        
        // now, let user 1 enter room
        enterRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK);

        // verify stanzas to existing occupants on the new user
        Stanza user1JoinedStanza = occupant2Queue.getNext();
        // should be from room + nick name
        assertEquals(ROOM1_JID.getFullQualifiedName() + "/nick", user1JoinedStanza.getFrom().getFullQualifiedName());
        // should be to the existing user
        assertEquals(OCCUPANT2_JID, user1JoinedStanza.getTo());
        
        XMLElement xElement = user1JoinedStanza.getSingleInnerElementsNamed("x");
        assertEquals(NamespaceURIs.XEP0045_MUC_USER, xElement.getNamespaceURI());
        
        // since this room is non-anonymous, x must contain an item element with the users full JID
        XMLElement itemElement = xElement.getSingleInnerElementsNamed("item");
        assertEquals(OCCUPANT1_JID.getFullQualifiedName(), itemElement.getAttributeValue("jid"));
        assertEquals("none", itemElement.getAttributeValue("affiliation"));
        assertEquals("participant", itemElement.getAttributeValue("role"));


        // verify stanzas to the new user on all existing users, including himself with status=110 element
        // own presence must be sent last
        // assert the stanza from the already existing user
        Stanza stanza = occupant1Queue.getNext();
        assertNotNull(stanza);
        assertEquals(ROOM1_JID.getFullQualifiedName() + "/Some nick", stanza.getFrom().getFullQualifiedName());
        assertEquals(OCCUPANT1_JID, stanza.getTo());

        // assert stanza from the joining user, must have extra status=110 element        
        stanza = occupant1Queue.getNext();
        assertNotNull(stanza);
        assertEquals(ROOM1_JID_WITH_NICK, stanza.getFrom());
        assertEquals(OCCUPANT1_JID, stanza.getTo());
        List<XMLElement> statusElements = stanza.getFirstInnerElement().getInnerElementsNamed("status");
        assertEquals(2, statusElements.size());
        assertEquals("100", statusElements.get(0).getAttributeValue("code"));
        assertEquals("110", statusElements.get(1).getAttributeValue("code"));
        
    }

    public void testDiscussionHistory() throws Exception {
        // add some messages
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.getHistory().append(StanzaBuilder.createMessageStanza(OCCUPANT2_JID, ROOM1_JID, MessageStanzaType.GROUPCHAT, null, "Body").getFinalStanza(), 
                new Occupant(OCCUPANT2_JID, "nick2", Affiliation.None, Role.Participant));
        room.getHistory().append(StanzaBuilder.createMessageStanza(OCCUPANT2_JID, ROOM1_JID, MessageStanzaType.GROUPCHAT, null, "Body2").getFinalStanza(), 
                new Occupant(OCCUPANT2_JID, "nick2", Affiliation.None, Role.Participant));
        room.getHistory().append(StanzaBuilder.createMessageStanza(OCCUPANT2_JID, ROOM1_JID, MessageStanzaType.GROUPCHAT, null, "Body3").getFinalStanza(), 
                new Occupant(OCCUPANT2_JID, "nick2", Affiliation.None, Role.Participant));
        
        // now, let user 1 enter room
        enterRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK, null, new History(2, null, null, null), false);

        Stanza stanza = occupant1Queue.getNext();
        // first stanza should be room presence
        assertNotNull(stanza);
        assertEquals("presence", stanza.getName());
        
        stanza = occupant1Queue.getNext();
        // here we get the message history
        assertNotNull(stanza);
        assertEquals("message", stanza.getName());
        MessageStanza msgStanza = (MessageStanza) MessageStanza.getWrapper(stanza);
        assertEquals("Body2", msgStanza.getBody(null));

        stanza = occupant1Queue.getNext();
        // first stanza should be room presence
        assertNotNull(stanza);
        assertEquals("message", stanza.getName());
        msgStanza = (MessageStanza) MessageStanza.getWrapper(stanza);
        assertEquals("Body3", msgStanza.getBody(null));
        
        // we only requested two messages
        assertNull(occupant1Queue.getNext());
}

}
