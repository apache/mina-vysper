package org.apache.vysper.xmpp.server;

import junit.framework.TestCase;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.stanza.Stanza;

import java.io.InputStream;

import static org.mockito.Mockito.mock;

/**
 */
public class XMPPServerTestCase extends TestCase {

    public void testDomainName() {
        expectConstructorIAE("");
        expectConstructorIAE(null);
        expectConstructorIAE(" ");
        expectConstructorIAE("vYsper.org");
    }

    private void expectConstructorIAE(String domain) {
        try {
            new XMPPServer(domain);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // success, fail through
        }
    }
    
    public void testNominalPath() throws Exception {
        XMPPServer tested = new XMPPServer("foo.com");
        tested.setStorageProviderRegistry(new MemoryStorageProviderRegistry());
        tested.setTLSCertificateInfo((InputStream) null, "");
        tested.addEndpoint(mock(Endpoint.class));

        tested.start();
    }
    
    public void testStanzaProcessorOverride() throws Exception {
        XMPPServer tested = new XMPPServer("foo.com");
        StanzaProcessor stanzaProcessor = mock(StanzaProcessor.class);
        tested.setStorageProviderRegistry(new MemoryStorageProviderRegistry());
        tested.setTLSCertificateInfo((InputStream) null, "");
        tested.addEndpoint(mock(Endpoint.class));
        
        tested.setStanzaProcessor(stanzaProcessor);
        
        tested.start();
        
        assertEquals(stanzaProcessor, tested.getServerRuntimeContext().getStanzaProcessor());
    }
    
}
