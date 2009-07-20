package org.apache.vysper.xmpp.authorization;

import junit.framework.TestCase;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.xmlfragment.XMLSemanticError;
import org.apache.commons.codec.binary.Base64;

/**
 */
public class PlainTestCase extends TestCase {
    protected SessionStateHolder stateHolder;

    public void testPlainEmpty() throws XMLSemanticError {

        Stanza stanza = new StanzaBuilder("plain").addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL).getFinalStanza();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "malformed-request");
    }

    public void testPlainNonBASE64() throws XMLSemanticError {

        Stanza stanza = new StanzaBuilder("plain").addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)
                .addText("aEflkejidkj==")
                .getFinalStanza();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "malformed-request");
    }

    public void testPlainNonExistingUser() throws XMLSemanticError {

        Stanza stanza = new StanzaBuilder("plain").addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)
                .addText(encode("dieter", "schluppkoweit"))
                .getFinalStanza();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "not-authorized");
    }

    public void testPlainNotExistingUser() throws XMLSemanticError {

        Stanza stanza = new StanzaBuilder("plain").addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)
                .addText(encode("dieter", "schluppkoweit"))
                .getFinalStanza();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "not-authorized");
    }

    public void testPlainNoUserPasswordCombination() throws XMLSemanticError {

        String innerText = new String(Base64.encodeBase64("continuous".getBytes()));

        Stanza stanza = new StanzaBuilder("plain").addNamespaceAttribute(NamespaceURIs.URN_IETF_PARAMS_XML_NS_XMPP_SASL)
                .addText(innerText).getFinalStanza();

        Stanza response = startMechanism(stanza);
        assertResponse(response, "malformed-request");
    }

    private Stanza startMechanism(Stanza finalStanza) {
        Plain plain = new Plain();
        stateHolder = new SessionStateHolder();
        Stanza response = plain.started(new TestSessionContext(stateHolder), stateHolder, finalStanza);
        return response;
    }

    private void assertResponse(Stanza response, String failureType) throws XMLSemanticError {
        assertTrue(response.getVerifier().nameEquals("failure"));
        assertNotNull(response.getSingleInnerElementsNamed(failureType));
        assert stateHolder.getState() != SessionState.AUTHENTICATED; 
    }

    private String encode(String username, String password) {
        return new String(Base64.encodeBase64(('\0' + username + '\0' + password).getBytes()));
    }
}
