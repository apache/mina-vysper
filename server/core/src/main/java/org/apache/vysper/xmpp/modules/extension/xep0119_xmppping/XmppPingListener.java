package org.apache.vysper.xmpp.modules.extension.xep0119_xmppping;

public interface XmppPingListener {
    void pong();
    void timeout();
}