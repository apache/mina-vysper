package org.apache.vysper.xmpp.server.s2s;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerNotFoundException;
import org.apache.vysper.xmpp.delivery.failure.RemoteServerTimeoutException;

public interface XMPPServerConnectorRegistry {

    XMPPServerConnector connect(Entity server) throws RemoteServerNotFoundException,
            RemoteServerTimeoutException;

    void close();

}