package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.AbstractMUCHandlerTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

/**
 */
public abstract class AbstractMUCOccupantDiscoTestCase extends AbstractMUCHandlerTestCase {

    private Stanza sendDisco(Stanza stanza) throws ProtocolException {
        ResponseStanzaContainer container = handler.execute(stanza,
                sessionContext.getServerRuntimeContext(), true, sessionContext,
                null);
        if (container != null) {
            return container.getResponseStanza();
        } else {
            return null;
        }
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        ServiceCollector serviceCollector = new ServiceCollector();
        sessionContext.getServerRuntimeContext().registerServerRuntimeContextService(serviceCollector);
        
        MUCModule module = new MUCModule(SUBDOMAIN, conference);
        module.initialize(sessionContext.getServerRuntimeContext());
        sessionContext.getServerRuntimeContext().registerComponent(module);
        
        serviceCollector.addComponentInfoRequestListener(module);
        serviceCollector.addItemRequestListener(module);

    }

    protected abstract String getNamespace();
    
    public void testDisco() throws Exception {
        // add occupants to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        StanzaBuilder request = StanzaBuilder.createIQStanza(OCCUPANT1_JID, new EntityImpl(ROOM1_JID, "Nick 2"), IQStanzaType.GET, "123");
        request.startInnerElement("query", getNamespace()).endInnerElement();

        // send message to room
        sendDisco(request.build());

        assertNull(occupant1Queue.getNext());
        Stanza stanza = occupant2Queue.getNext(); 
        assertNotNull(stanza);
        assertEquals(OCCUPANT1_JID, stanza.getFrom());
        assertEquals(OCCUPANT2_JID, stanza.getTo());
        assertEquals("get", stanza.getAttributeValue("type"));
        assertEquals("123", stanza.getAttributeValue("id"));
        XMLElement query = stanza.getFirstInnerElement();
        assertNotNull(query);
        assertEquals(getNamespace(), query.getNamespaceURI());
    }
}
