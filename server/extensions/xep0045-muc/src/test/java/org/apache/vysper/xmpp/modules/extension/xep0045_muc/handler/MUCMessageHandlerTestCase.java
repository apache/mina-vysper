package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.PresenceStanza;
import org.apache.vysper.xmpp.stanza.PresenceStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;
import org.apache.vysper.xmpp.xmlfragment.XMLText;

/**
 */
public class MUCMessageHandlerTestCase extends AbstractMUCHandlerTestCase {
    
    private Stanza sendMessage(Entity occupantJid, Entity roomJid, String type, String body) throws ProtocolException {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createMessageStanza(occupantJid, roomJid, null, body);
        if(type != null) {
            stanzaBuilder.addAttribute("type", type);
        }
        
        Stanza messageStanza = stanzaBuilder.getFinalStanza();
        ResponseStanzaContainer container = handler.execute(messageStanza, sessionContext.getServerRuntimeContext(), true, sessionContext, null);
        if(container != null) {
            return container.getResponseStanza();
        } else {
            return null;
        }
    }
    
    private void assertMessageErrorStanza(Stanza response, Entity from, Entity to,
            String type, String errorName, List<XMLElement> expectedInnerElements) {
        assertErrorStanza(response, "message", from, to, type, errorName, expectedInnerElements);
    }
    
    public void testMessageWithNoVoice() throws Exception {
        Room room = conference.findOrCreateRoom(room1Jid, "Room 1");
        Occupant occupant = room.addOccupant(occupant1Jid, "nick");
        // make sure the occupant has no voice
        occupant.setRole(Role.Visitor);

        
        testNotAllowedMessage(room, "forbidden");
    }
    
    public void testMessageUserNotOccupant() throws Exception {
        Room room = conference.findOrCreateRoom(room1Jid, "Room 1");
        // do not add user to room
        
        testNotAllowedMessage(room, "not-acceptable");
    }
    
    private void testNotAllowedMessage(Room room, String expectedErrorName) throws Exception {
        String body = "Message body";
        
        
        // now, let user 2 exit room
        Stanza errorStanza = sendMessage(occupant1Jid, room1JidWithNick, "groupchat", body);

        XMLText text = new XMLText(body);
        XMLElement expectedBody = new XMLElement("body", null, (Attribute[])null, new XMLFragment[]{text});
        assertMessageErrorStanza(errorStanza, room1Jid, occupant1Jid, "modify", expectedErrorName, Arrays.asList(expectedBody));
        
        // no message should be relayed
        assertNull(occupant1Queue.getNext());
        assertNull(occupant2Queue.getNext());
    }
    
    public void testMessageToRoomWithRelays() throws Exception {
        String body = "Message body";
        
        // add occupants to the room
        Room room = conference.findOrCreateRoom(room1Jid, "Room 1");
        room.addOccupant(occupant1Jid, "nick");
        room.addOccupant(occupant2Jid, "Nick 2");
        
        // now, let user 2 exit room
        sendMessage(occupant1Jid, room1JidWithNick, "groupchat", body);

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(room1JidWithNick, occupant1Jid, "groupchat", body, occupant1Queue.getNext());
        assertMessageStanza(room1JidWithNick, occupant2Jid, "groupchat", body, occupant2Queue.getNext());
    }

    private void assertMessageStanza(Entity from, Entity to, String type, String body, Stanza stanza) throws XMLSemanticError {
        assertEquals(from, stanza.getFrom());
        assertEquals(to, stanza.getTo());
        if(type != null) {
            assertEquals(type, stanza.getAttributeValue("type"));
        }
        
        XMLElement bodyElement = stanza.getSingleInnerElementsNamed("body");
        assertEquals(body, bodyElement.getInnerText().getText());
    }

    @Override
    protected StanzaHandler createHandler() {
        return new MUCMessageHandler(conference);
    }
}
