package org.apache.vysper.xmpp.server.s2s;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerTimeoutException;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionContext;

public interface XMPPServerConnectorRegistry {

    XMPPServerConnector connect(Entity server) throws RemoteServerNotFoundException,
            RemoteServerTimeoutException;

    XMPPServerConnector connectForDialback(Entity server, SessionContext sessionContext, SessionStateHolder sessionStateHolder) throws RemoteServerNotFoundException,
    RemoteServerTimeoutException;

    
    void close();

}