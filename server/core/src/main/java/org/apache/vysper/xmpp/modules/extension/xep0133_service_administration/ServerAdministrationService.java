package org.apache.vysper.xmpp.modules.extension.xep0133_service_administration;

import org.apache.vysper.xmpp.addressing.Entity;

import java.util.Collection;

/**
 */
public interface ServerAdministrationService {

    public static final String SERVICE_ID = "ServerAdministrationService";
    
    void setAddAdminJIDs(Collection<Entity> admins);

    void setAddAdmins(Collection<String> admins);

    boolean isAdmin(Entity adminCandidate);
}
