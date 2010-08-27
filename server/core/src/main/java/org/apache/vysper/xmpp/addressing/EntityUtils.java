package org.apache.vysper.xmpp.addressing;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 * provides utility methods for {@link org.apache.vysper.xmpp.addressing.Entity}
 */
public class EntityUtils {

    /**
     * checks a JID to be addressed to the server's domain
     * @param toVerify - JID to be verified 
     * @param serverJID - JID of the server
     * @return TRUE iff toVerify JID equals the server's JID
     */
    public static boolean isAddressingServer(Entity toVerify, Entity serverJID) {
        return toVerify.getDomain().equals(serverJID.getDomain());
    }
    
    /**
     * checks a JID to be addressed to any of the server's component
     * @param toVerify - JID to be verified 
     * @param serverJID - JID of the server
     * @return TRUE iff toVerify's domain is a component of the given server JID
     */
    public static boolean isAddressingServerComponent(Entity toVerify, Entity serverJID) {
        return toVerify.getDomain().endsWith("." + serverJID.getDomain());
    }

    /**
     * creates a JID with only a domain part set, the domain part being assembled from a subdomain and the server domain.
     * @param subdomain
     * @param serverRuntimeContext to retrieve the server JID
     * @return subdomain.serverdomain.tld
     */
    public static Entity createComponentDomain(String subdomain, ServerRuntimeContext serverRuntimeContext) {
        try {
            return EntityImpl.parse(subdomain + "." + serverRuntimeContext.getServerEnitity().getDomain());
        } catch (EntityFormatException e) {
            // only happens when server entity is bad.
            throw new RuntimeException("could not create component domain", e);
        }
    }
}
