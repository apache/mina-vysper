package org.apache.vysper.xmpp.addressing;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 * provides utility methods for {@link org.apache.vysper.xmpp.addressing.Entity}
 */
public class EntityUtils {

    public static boolean isAddressingServer(Entity toVerify, Entity serverJID) {
        return toVerify.getDomain().equals(serverJID.getDomain());
    }
    
    public static boolean isAddressingServerComponent(Entity toVerify, Entity serverJID) {
        return toVerify.getDomain().endsWith("." + serverJID.getDomain());
    }

    public static Entity createComponentDomain(String subdomain, ServerRuntimeContext serverRuntimeContext) {
        try {
            return EntityImpl.parse(subdomain + "." + serverRuntimeContext.getServerEnitity().getDomain());
        } catch (EntityFormatException e) {
            // only happens when server entity is bad.
            throw new RuntimeException("could not create component domain", e);
        }
    }
}
