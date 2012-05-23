package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 */
public class ConferenceTestUtils {

    public static Room findOrCreateRoom(Conference conference, Entity jid, String name, RoomType... types) {
        Room room = conference.findRoom(jid);
        if (room == null) {
            room = conference.createRoom(jid, name, types);
        }
        return room;
    }

    public static MessageStanza toMessageStanza(Stanza stanza) {
        return (MessageStanza)XMPPCoreStanza.getWrapper(stanza);
    }
    
    public static MessageStanza createMessageStanza(final Entity from, final Entity to, final String body) {
        final Stanza stanza = StanzaBuilder.createMessageStanza(from, to, MessageStanzaType.GROUPCHAT, null, body).build();
        return toMessageStanza(stanza);
    }

    
}
