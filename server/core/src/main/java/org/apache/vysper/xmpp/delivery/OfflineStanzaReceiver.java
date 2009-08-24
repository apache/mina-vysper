package org.apache.vysper.xmpp.delivery;

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * receives stanzas addressed to offline receivers
 * TODO: this is more or less a placeholder interface currently.
 */
public interface OfflineStanzaReceiver {

    public void receive(Stanza stanza);
    
}
