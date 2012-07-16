package org.apache.vysper.xmpp.extension.xep0124;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.response.ServerResponses;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;

import java.util.Collection;

/**
 */
public class BoshStanzaUtils {
    
    /**
     * the empty BOSH response.
     * <p>
     * Looks like <code>&lt;body xmlns='http://jabber.org/protocol/httpbind'/&gt;</code>
     */
    protected static final Stanza EMPTY_BOSH_RESPONSE = createBoshStanzaBuilder().build();
    
    protected static final Stanza RESTART_BOSH_RESPONSE = wrapStanza(new ServerResponses().getFeaturesForSession());
    
    protected static final Stanza TERMINATE_BOSH_RESPONSE = createTerminateResponse(null).build();

    /**
     * Creates a new BOSH response builder
     */
    public static StanzaBuilder createBoshStanzaBuilder() {
        return new StanzaBuilder("body", NamespaceURIs.XEP0124_BOSH);
    }

    /**
     * Creates a BOSH response by wrapping a stanza in a &lt;body/&gt; element
     * @param stanza the XMPP stanza to wrap
     * @return the BOSH response
     */
    public static Stanza wrapStanza(Stanza stanza) {
        StanzaBuilder body = createBoshStanzaBuilder();
        body.addPreparedElement(stanza);
        return body.build();
    }

    /**
     * Creates a unified BOSH response by merging BOSH responses, this is useful when sending more than one message as
     * a response to a HTTP request.
     * @param response1 the first BOSH response to merge
     * @param response2 the second BOSH response to merge
     * @return the merged BOSH response
     */
    public static Stanza mergeResponses(Collection<Stanza> mergeCandidates) {
        if (mergeCandidates == null || mergeCandidates.size() == 0) {
            return null;
        }
        if (mergeCandidates.size() == 1) {
            // nothing to merge with only one mergee. return unmerged
            return mergeCandidates.iterator().next();
        }
        StanzaBuilder body = createBoshStanzaBuilder();
        for (Stanza mergee : mergeCandidates) {
            if (mergee == null) continue;
            for (XMLElement element : mergee.getInnerElements()) {
                body.addPreparedElement(element);
            }
        }
        return body.build();
    }

    /**
     * Creates a session termination BOSH response
    */
    public static StanzaBuilder createTerminateResponse(String condition) {
        StanzaBuilder stanzaBuilder = createBoshStanzaBuilder();
        stanzaBuilder.addAttribute("type", "terminate");
        if (condition != null) stanzaBuilder.addAttribute("condition", condition);
        return stanzaBuilder;
    }

    /**
     * Creates a session termination BOSH response signalling a broken session
    */
    public static StanzaBuilder createBrokenSessionReport(long report, long delta) {
        StanzaBuilder stanzaBuilder = createTerminateResponse(null);
        stanzaBuilder = stanzaBuilder.addAttribute("report", Long.toString(report));
        stanzaBuilder = stanzaBuilder.addAttribute("time", Long.toString(delta));
        return stanzaBuilder;
    }

    /**
     * Adds a top-level custom attribute to a BOSH body after the stanza is already built.
     * 
     * @param stanza the BOSH body
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @return a new BOSH body identical with the one provided except it also has the newly added attribute
     */
    public static Stanza addAttribute(Stanza stanza, String attributeName, String attributeValue) {
        StanzaBuilder stanzaBuilder = createBoshStanzaBuilder();
        for (Attribute attr : stanza.getAttributes()) {
            stanzaBuilder.addAttribute(attr);
        }
        stanzaBuilder.addAttribute(attributeName, attributeValue);
        for (XMLElement element : stanza.getInnerElements()) {
            stanzaBuilder.addPreparedElement(element);
        }
        return stanzaBuilder.build();
    }
}
