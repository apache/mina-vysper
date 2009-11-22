package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.disco;

import java.util.List;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.AbstractPublishSubscribeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.AbstractStanzaGenerator;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.modules.servicediscovery.handler.DiscoItemIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;


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
        DefaultDiscoInfoStanzaGenerator sg = (DefaultDiscoInfoStanzaGenerator)getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService.getBareJID(), "id123");

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
        root.add(new LeafNode(serviceConfiguration, "news", "News", client));
        root.add(new LeafNode(serviceConfiguration, "blogs", "Blogs", client));
        
        DefaultDiscoInfoStanzaGenerator sg = (DefaultDiscoInfoStanzaGenerator)getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService.getBareJID(), "id123");

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
    
    public void testNodeItemsNone() throws Exception {
        root.add(new LeafNode(serviceConfiguration, "news", "News", client));
        
        DefaultDiscoInfoStanzaGenerator sg = (DefaultDiscoInfoStanzaGenerator)getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService.getBareJID(), "id123", "news");

        ResponseStanzaContainer result = sendStanza(stanza, true);
        assertTrue(result.hasResponse());
        IQStanza response = new IQStanza(result.getResponseStanza());

        assertEquals(IQStanzaType.RESULT.value(),response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match
        
        // get the query Element
        XMLElement query = response.getFirstInnerElement();
        List<XMLElement> inner = query.getInnerElements();

        assertEquals("query", query.getName());
        assertEquals("news", query.getAttributeValue("node"));
        
        // since we have no messages, there should be no items.
        assertEquals(0, inner.size());
    }
    
    public void testNodeItemsSome() throws Exception {
        LeafNode node = new LeafNode(serviceConfiguration, "news", "News", client);
        root.add(node);
        
        XMLElement item1 = new XMLElement("namespace1", "item1", null,(Attribute[])null, (XMLFragment[])null);
        XMLElement item2 = new XMLElement("namespace2", "item2", null,(Attribute[])null, (XMLFragment[])null);
        XMLElement item3 = new XMLElement("namespace3", "item3", null,(Attribute[])null, (XMLFragment[])null);
        node.publish(client, relay, "itemid1", item1);
        Thread.sleep(10);
        node.publish(client, relay, "itemid2", item1); // publish this one with the same id as the next one (overwritten by the next)
        node.publish(client, relay, "itemid2", item2); // overwrite the prev. item (use the same itemid)
        Thread.sleep(10);
        node.publish(client, relay, "itemid3", item3);
        
        
        DefaultDiscoInfoStanzaGenerator sg = (DefaultDiscoInfoStanzaGenerator)getDefaultStanzaGenerator();
        Stanza stanza = sg.getStanza(client, pubsubService.getBareJID(), "id123", "news");

        ResponseStanzaContainer result = sendStanza(stanza, true);
        assertTrue(result.hasResponse());
        IQStanza response = new IQStanza(result.getResponseStanza());

        assertEquals(IQStanzaType.RESULT.value(),response.getType());

        assertEquals("id123", response.getAttributeValue("id")); // IDs must match
        
        // get the query Element
        XMLElement query = response.getFirstInnerElement();
        List<XMLElement> inner = query.getInnerElements();

        assertEquals("query", query.getName());
        assertEquals("news", query.getAttributeValue("node"));
        
        // since we have no messages, there should be no items.
        assertEquals(3, inner.size());
        
        // the items should be returned in the reversed ordering of sending, make sure they are.
        boolean bItem1 = false;
        boolean bItem2 = false;
        boolean bItem3 = false;
        for(XMLElement el : inner) {
            if(el.getName().equals("item") 
                    && el.getAttributeValue("jid").equals(serverEntity.getFullQualifiedName())) {
                if(!bItem1 && el.getAttributeValue("name").equals("itemid1")) {
                    bItem1 = true;
                } else if(bItem1 && !bItem2 && el.getAttributeValue("name").equals("itemid2")) {
                    bItem2 = true;
                } else if(bItem1 && bItem2 && !bItem3 && el.getAttributeValue("name").equals("itemid3")) {
                    bItem3 = true;
                }
            }
        }
        
        assertTrue(bItem1);
        assertTrue(bItem2);
        assertTrue(bItem3);
    }
    
    class DefaultDiscoInfoStanzaGenerator extends AbstractStanzaGenerator {
        @Override
        protected StanzaBuilder buildInnerElement(Entity client, Entity pubsub, StanzaBuilder sb, String node) {
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

        public Stanza getStanza(Entity client, Entity pubsub, String id) {
            StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(client, pubsub, getStanzaType(), id);
            stanzaBuilder.startInnerElement("query");
            stanzaBuilder.addNamespaceAttribute(getNamespace());

            stanzaBuilder.endInnerElement();

            return stanzaBuilder.getFinalStanza();
        }
        
        @Override
        public Stanza getStanza(Entity client, Entity pubsub, String id, String node) {
            StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(client, pubsub, getStanzaType(), id);
            stanzaBuilder.startInnerElement("query");
            stanzaBuilder.addNamespaceAttribute(getNamespace());
            stanzaBuilder.addAttribute("node", node);

            stanzaBuilder.endInnerElement();

            return stanzaBuilder.getFinalStanza();
        }
    }
}
