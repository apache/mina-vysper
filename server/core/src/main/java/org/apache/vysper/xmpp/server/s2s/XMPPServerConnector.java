package org.apache.vysper.xmpp.server.s2s;

import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.writer.StanzaWriter;

public interface XMPPServerConnector extends StanzaWriter {

    void write(Stanza stanza);

}