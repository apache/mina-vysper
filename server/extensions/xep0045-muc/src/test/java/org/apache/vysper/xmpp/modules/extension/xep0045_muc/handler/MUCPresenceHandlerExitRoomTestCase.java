package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;

/**
 */
public class MUCPresenceHandlerExitRoomTestCase extends AbstractMUCHandlerTestCase {
    
    private Stanza exitRoom(Entity occupantJid, Entity roomJid) throws ProtocolException {
        return exitRoom(occupantJid, roomJid, null);
    }
    
    private Stanza exitRoom(Entity occupantJid, Entity roomJid, String status) throws ProtocolException {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(occupantJid, roomJid, null, PresenceStanzaType.UNAVAILABLE, null, status);

        Stanza presenceStanza = stanzaBuilder.build();
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
    
    public void testExitRoom() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.addOccupant(OCCUPANT1_JID, "Nick1");
        room.addOccupant(OCCUPANT2_JID, "Nick2");

        assertNull(exitRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK));

        assertEquals(1, room.getOccupants().size());
        Occupant occupant = room.getOccupants().iterator().next();
        
        assertEquals(OCCUPANT2_JID, occupant.getJid());
    }

    public void testExitNonexistingRoom() throws Exception {
        // Quietly ignore
        assertNull(exitRoom(OCCUPANT1_JID, ROOM2_JID_WITH_NICK));
    }

    public void testExitRoomWithoutEntering() throws Exception {
        // Exit a room where the user is not a participant, quietly ignore
        assertNull(exitRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK));
    }

    public void testTemporaryRoomDeleted() throws Exception {
        // Room1 is temporary
        Room room = conference.findRoom(ROOM1_JID);
        assertTrue(room.isRoomType(RoomType.Temporary));
        room.addOccupant(OCCUPANT1_JID, "Nick1");

        // exit room, room should be deleted
        assertNull(exitRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK));
        assertNull(conference.findRoom(ROOM1_JID));
    }
    

    public void testPersistentRoomNotDeleted() throws Exception {
        // Room2 is persistent
        Room room = conference.createRoom(ROOM2_JID, "Room 2", RoomType.Persistent);
        room.addOccupant(OCCUPANT1_JID, "Nick1");

        // exit room, room should be deleted
        assertNull(exitRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK));
        assertNotNull(conference.findRoom(ROOM1_JID));
    }
    
    public void testExitRoomWithRelays() throws Exception {
        // add occupants to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "Nick 1");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");
        
        // now, let user 1 exit room
        exitRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK);

        // verify stanzas to existing occupants on the exiting user
        assertExitPresenceStanza(ROOM1_JID, "Nick 1", OCCUPANT1_JID, occupant1Queue.getNext(), null, true);
        assertExitPresenceStanza(ROOM1_JID, "Nick 1", OCCUPANT2_JID, occupant2Queue.getNext(), null, false);
    }
    
    public void testExitRoomWithRelaysWithStatus() throws Exception {
        String statusMessage = "Custom status";
        
        // add occupants to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "Nick 1");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");
        
        // now, let user 1 exit room
        exitRoom(OCCUPANT1_JID, ROOM1_JID_WITH_NICK, statusMessage);

        // verify stanzas to existing occupants on the exiting user
        assertExitPresenceStanza(ROOM1_JID, "Nick 1", OCCUPANT1_JID, occupant1Queue.getNext(), statusMessage, true);
        assertExitPresenceStanza(ROOM1_JID, "Nick 1", OCCUPANT2_JID, occupant2Queue.getNext(), statusMessage, false);
    }

    private void assertExitPresenceStanza(Entity roomJid, String nick, Entity to, Stanza stanza, String expectedStatus, boolean own) throws XMLSemanticError {
        // should be from room + nick name
        assertEquals(roomJid.getFullQualifiedName() + "/" + nick, stanza.getFrom().getFullQualifiedName());
        // should be to the existing user
        assertEquals(to, stanza.getTo());
        
        assertEquals("unavailable", stanza.getAttributeValue("type"));
        
        XMLElement xElement = stanza.getSingleInnerElementsNamed("x");
        assertEquals(NamespaceURIs.XEP0045_MUC_USER, xElement.getNamespaceURI());
        
        // since this room is non-anonymous, x must contain an item element with the users full JID
        XMLElement itemElement = xElement.getSingleInnerElementsNamed("item");
        assertEquals("none", itemElement.getAttributeValue("affiliation"));
        assertEquals("none", itemElement.getAttributeValue("role"));
        
        if(own) {
            List<XMLElement> statuses = xElement.getInnerElementsNamed("status");
            assertEquals(1, statuses.size());
            assertEquals("110", statuses.get(0).getAttributeValue("code"));
        }
        
        if(expectedStatus != null) {
            List<XMLElement> statuses = xElement.getInnerElementsNamed("status");
            assertEquals(1, statuses.size());
            assertEquals(expectedStatus, statuses.get(0).getInnerText().getText());
            
        }
    }
}
