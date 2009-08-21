package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 */
public class MUCPresenceHandlerChangeNickTestCase extends AbstractMUCHandlerTestCase {

    private Stanza changeNick(Entity occupantJid, Entity roomWithNickJid) throws ProtocolException {
        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(occupantJid, roomWithNickJid, null, null, null, null);
        stanzaBuilder.startInnerElement("x").addNamespaceAttribute(NamespaceURIs.XEP0045_MUC);
        
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
    
    public void testChangeNick() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "nick 2");

        changeNick(OCCUPANT1_JID, new EntityImpl(ROOM1_JID, "new nick"));
        
        Occupant occupant = room.findOccupantByJID(OCCUPANT1_JID);
        assertEquals("new nick", occupant.getName());

        MUCUserItem unavailbleItem = new MUCUserItem(OCCUPANT1_JID, "new nick", Affiliation.None, Role.Participant);
        assertPresenceStanza(occupant1Queue.getNext(), new EntityImpl(ROOM1_JID, "nick"), OCCUPANT1_JID, "unavailable", 
                Arrays.asList(unavailbleItem), Arrays.asList(330, 110));
        assertPresenceStanza(occupant2Queue.getNext(), new EntityImpl(ROOM1_JID, "nick"), OCCUPANT2_JID, "unavailable", 
                Arrays.asList(unavailbleItem), Arrays.asList(330));

        MUCUserItem availbleItem = new MUCUserItem(OCCUPANT1_JID, null, Affiliation.None, Role.Participant);
        assertPresenceStanza(occupant1Queue.getNext(), new EntityImpl(ROOM1_JID, "new nick"), OCCUPANT1_JID, null, 
                Arrays.asList(availbleItem), Arrays.asList(110));
        assertPresenceStanza(occupant2Queue.getNext(), new EntityImpl(ROOM1_JID, "new nick"), OCCUPANT2_JID, null, 
                Arrays.asList(availbleItem), null);
    }
    
    public void testChangeNickWithDuplicateNick() throws Exception {
        Room room = conference.findRoom(ROOM1_JID);
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "nick 2");
        
        Stanza error = changeNick(OCCUPANT1_JID, new EntityImpl(ROOM1_JID,"nick 2"));
        
        assertNotNull(error);
    }

    private void assertPresenceStanza(Stanza stanza, Entity expectedFrom, Entity expectedTo, String expectedType,
            List<MUCUserItem> expectedItems, List<Integer> expectedStatuses) {

        assertNotNull(stanza);
        assertEquals(expectedFrom, stanza.getFrom());
        assertEquals(expectedTo, stanza.getTo());
        assertEquals(expectedType, stanza.getAttributeValue("type"));
        
        XMLElement xElm = stanza.getFirstInnerElement();
        assertEquals(NamespaceURIs.XEP0045_MUC_USER, xElm.getNamespaceURI());
        
        Iterator<XMLElement> itemElements = xElm.getInnerElements().iterator();
        for(MUCUserItem item : expectedItems) {
            XMLElement itemElm = itemElements.next();
            
            assertEquals(item.getJid().getFullQualifiedName(), itemElm.getAttributeValue("jid"));
            assertEquals(item.getNick(), itemElm.getAttributeValue("nick"));
            assertEquals(item.getAffiliation().toString(), itemElm.getAttributeValue("affiliation"));
            assertEquals(item.getRole().toString(), itemElm.getAttributeValue("role"));
        }
    }
    
}
