package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import org.apache.vysper.xmpp.addressing.Entity;

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

}
