package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import static org.apache.vysper.xmpp.stanza.MessageStanzaType.GROUPCHAT;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.stanza.Stanza;


/**
 */
public class MUCSubjectMessageHandlerTestCase extends AbstractMUCMessageHandlerTestCase {

    private static final String SUBJECT = "Subject";

    public void testChangeSubjectNonModeratorAllowed() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM2_JID, "Room 2", RoomType.OpenSubject);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to room
        assertNull(sendMessage(OCCUPANT1_JID, ROOM2_JID, GROUPCHAT, null, null, SUBJECT));

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM2_JID_WITH_NICK, OCCUPANT1_JID, "groupchat", null, SUBJECT, null,
                occupant1Queue.getNext());
        assertMessageStanza(ROOM2_JID_WITH_NICK, OCCUPANT2_JID, "groupchat", null, SUBJECT, null,
                occupant2Queue.getNext());
    }

    public void testChangeSubject() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM2_JID, "Room 2");
        room.addOccupant(OCCUPANT1_JID, "nick").setRole(Role.Moderator);
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to room
        assertNull(sendMessage(OCCUPANT1_JID, ROOM2_JID, GROUPCHAT, null, null, SUBJECT));

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM2_JID_WITH_NICK, OCCUPANT1_JID, "groupchat", null, SUBJECT, null,
                occupant1Queue.getNext());
        assertMessageStanza(ROOM2_JID_WITH_NICK, OCCUPANT2_JID, "groupchat", null, SUBJECT, null,
                occupant2Queue.getNext());
    }

    public void testChangeSubjectNonModerator() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM2_JID, "Room 2");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to room
        Stanza error = sendMessage(OCCUPANT1_JID, ROOM2_JID, GROUPCHAT, null, null, SUBJECT);

        assertMessageErrorStanza(error, ROOM2_JID, OCCUPANT1_JID, "auth", "forbidden", 
                new XMLElementBuilder("subject").addText(SUBJECT).build());
        
        assertNull(occupant1Queue.getNext());
        assertNull(occupant2Queue.getNext());
    }


}
