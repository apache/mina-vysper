package org.apache.vysper.xmpp.modules.roster.persistence;

import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;

/**
 */
public class RosterManagerUtils {

    /**
     * retrieves the roster manager from the server runtime context and throws exception if not present
     *
     * @param serverRuntimeContext
     * @param sessionContext
     * @return roster manager - will not be NULL
     * @throws RuntimeException iff roster manager cannot be retrieved
     */
    public static RosterManager getRosterInstance(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        RosterManager rosterManager;
        try {
            rosterManager = (RosterManager)serverRuntimeContext.getStorageProvider(RosterManager.class);
        } catch (Exception e) {
            // System.err.println("failed to retrieve roster manager for session id = " + sessionContext.getSessionId());
            String sessionId = sessionContext == null ? "NO_SESSION" : sessionContext.getSessionId();
            throw new RuntimeException("failed to retrieve roster manager for session id = " + sessionId);
        }
        return rosterManager;
    }
}
