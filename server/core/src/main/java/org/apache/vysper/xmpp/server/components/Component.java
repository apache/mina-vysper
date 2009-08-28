package org.apache.vysper.xmpp.server.components;

import org.apache.vysper.xmpp.server.SessionContext;

/**
 * a component is a server subsystem providing a dedicated extension.
 * components operate on their own subdomain, e.g. conference.vysper.org for MUC.
 * components have a dedicated context in which they receive stanzas 
 */
public interface Component {
    
    String getSubdomain();
    
    SessionContext getSessionContext();
}
