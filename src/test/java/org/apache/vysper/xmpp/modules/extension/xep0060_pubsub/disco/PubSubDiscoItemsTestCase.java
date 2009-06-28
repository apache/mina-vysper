package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.disco;

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.AbstractStanzaGenerator;
import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoItemIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;


public class PubSubDiscoItemsTestCase extends AbstractPublishSubscribeTestCase {
    
    @Override
    protected AbstractStanzaGenerator getDefaultStanzaGenerator() {
        return new DefaultDiscoInfoStanzaGenerator();
    }

    @Override
    protected IQHandler getHandler() {
        return new DiscoItemIQHandler();
    }

    
    public void testNoItems() {
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsub.getBareJID(), "id123");

        ResponseStanzaContainer result = sendStanza(stanza, true);
        assertTrue(result.hasResponse());
        IQStanza response = new IQStanza(result.getResponseStanza());

        assertEquals(IQStanzaType.RESULT.value(),response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match

        // get the query Element
        XMLElement query = response.getFirstInnerElement();
        List<XMLElement> inner = query.getInnerElements();

        assertEquals("query", query.getName());
        
        // since we have no nodes, there should be no items.
        assertEquals(0, inner.size());
    }
    
    public void testSomeItems() throws Exception {
        root.createNode(EntityImpl.parse("pubsub.vysper.org/news"), "News");
        root.createNode(EntityImpl.parse("pubsub.vysper.org/blogs"), "Blogs");
        
        AbstractStanzaGenerator sg = getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsub.getBareJID(), "id123");

        ResponseStanzaContainer result = sendStanza(stanza, true);
        assertTrue(result.hasResponse());
        IQStanza response = new IQStanza(result.getResponseStanza());

        assertEquals(IQStanzaType.RESULT.value(),response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match
        
        // get the query Element
        XMLElement query = response.getFirstInnerElement();
        List<XMLElement> inner = query.getInnerElements();

        assertEquals("query", query.getName());
        
        // since we have no nodes, there should be no items.
        assertEquals(2, inner.size());
//        
//        // ordering etc. is unknown; step through all subelements and pick the ones we need
        XMLElement news = null;
        XMLElement blogs = null;
        for(XMLElement el : inner) {
            if(el.getName().equals("item") /* && el.getNamespace().equals(NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS) */) { //TODO enable after fixing the namespace bug
                if(el.getAttributeValue("jid").equals(serverEntity.getFullQualifiedName())
                        && el.getAttributeValue("node").equals("news")
                        && el.getAttributeValue("name").equals("News")) {
                    news = el;
                } else if(el.getAttributeValue("jid").equals(serverEntity.getFullQualifiedName())
                        && el.getAttributeValue("node").equals("blogs")
                        && el.getAttributeValue("name").equals("Blogs")) {
                    blogs = el;
                }
            }
        }
        
        // make sure they were there (booleans would have sufficed)
        assertNotNull(news);
        assertNotNull(blogs);
    }
    
    class DefaultDiscoInfoStanzaGenerator extends AbstractStanzaGenerator {
        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb) {
            return sb;
        }

        @Override
        protected String getNamespace() {
            return NamespaceURIs.XEP0030_SERVICE_DISCOVERY_ITEMS;
        }

        @Override
        protected IQStanzaType getStanzaType() {
            return IQStanzaType.GET;
        }
        
        @Override
        public Stanza getStanza(Entity client, Entity pubsub, String id) {
            StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(client, pubsub, getStanzaType(), id);
            stanzaBuilder.startInnerElement("query");
            stanzaBuilder.addNamespaceAttribute(getNamespace());

            buildInnerElement(client, pubsub, stanzaBuilder);

            stanzaBuilder.endInnerElement();

            return stanzaBuilder.getFinalStanza();
        }
    }
}
