package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
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
        return enterRoom(occupantJid, roomJid, null);
    }
    
    private Stanza enterRoom(Entity occupantJid, Entity roomJid, String password) throws ProtocolException {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(occupantJid, roomJid, null, null, null, null);
        stanzaBuilder.startInnerElement("x").addNamespaceAttribute(NamespaceURIs.XEP0045_MUC);
        if(password != null) {
            stanzaBuilder.startInnerElement("password").addText(password).endInnerElement();
        }
        
        stanzaBuilder.endInnerElement();
        Stanza presenceStanza = stanzaBuilder.getFinalStanza();
        ResponseStanzaContainer container = handler.execute(presenceStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null);
        if(container != null) {
            return container.getResponseStanza();
        } else {
            return null;
        }
    }
    

    @Override
    protected StanzaHandler createHandler() {
        return new MUCPresenceHandler(conference);
    }
    
    public void testEnterExistingRoom() throws Exception {
        Room room = conference.findRoom(room1Jid);
        assertEquals(0, room.getOccupants().size());

        enterRoom(occupant1Jid, room1JidWithNick);

        assertEquals(1, room.getOccupants().size());
        Occupant occupant = room.getOccupants().iterator().next();
        
        assertEquals(occupant1Jid, occupant.getJid());
        assertEquals("nick", occupant.getName());
    }
    
    public void testEnterRoomWithDuplicateNick() throws Exception {
        assertNull(enterRoom(occupant1Jid, room1JidWithNick));
        Stanza error = enterRoom(occupant2Jid, room1JidWithNick);
        
        assertNotNull(error);
    }

    public void testEnterNonExistingRoom() throws Exception {
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
    
    public void testEnterWithoutNick() throws Exception {
        // try entering without a nick
        PresenceStanza response = (PresenceStanza) enterRoom(occupant1Jid, room1Jid);

        assertPresenceErrorStanza(response, room1Jid, occupant1Jid, "modify", "jid-malformed");
    }
    
    public void testEnterWithPassword() throws Exception {
        Room room = conference.createRoom(room2Jid, "Room 1", RoomType.PasswordProtected);
        room.setPassword("secret");

        // no error should be returned
        assertNull(enterRoom(occupant1Jid, room2JidWithNick, "secret"));
        assertEquals(1, room.getOccupants().size());
    }
    
    public void testEnterWithoutPassword() throws Exception {
        Room room = conference.createRoom(room2Jid, "Room 1", RoomType.PasswordProtected);
        room.setPassword("secret");

        // try entering without a password
        PresenceStanza response = (PresenceStanza) enterRoom(occupant1Jid, room2JidWithNick);
        
        assertPresenceErrorStanza(response, room2Jid, occupant1Jid, "auth", "not-authorized");
    }

    private void assertPresenceErrorStanza(PresenceStanza response, Entity from, Entity to,
            String type, String errorName) {
        Attribute xmlns = new NamespaceAttribute(NamespaceURIs.XEP0045_MUC);
        XMLElement xElement = new XMLElement("x", null, Arrays.asList(xmlns), (XMLFragment[])null);
        assertErrorStanza(response, "presence", from, to, type, errorName, Arrays.asList(xElement));
    }

    
    public void testEnterRoomWithRelays() throws Exception {


        // add one occupant to the room
        Room room = conference.findOrCreateRoom(room1Jid, "Room 1");
        room.addOccupant(occupant1Jid, "Some nick");
        
        // now, let user 2 enter room
        enterRoom(occupant2Jid, room1JidWithNick);

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
