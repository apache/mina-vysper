package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler.AbstractMUCHandlerTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoInfoIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

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
        MUCModule module = new MUCModule(MODULE_JID, conference);
        module.initialize(sessionContext.getServerRuntimeContext());
        serviceCollector.addInfoRequestListener(module);
        serviceCollector.addItemRequestListener(module);

        sessionContext.getServerRuntimeContext().registerServerRuntimeContextService(serviceCollector);
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
        sendDisco(request.getFinalStanza());

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
