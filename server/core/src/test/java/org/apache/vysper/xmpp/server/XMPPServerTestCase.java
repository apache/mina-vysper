package org.apache.vysper.xmpp.server;

import junit.framework.TestCase;

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
}
