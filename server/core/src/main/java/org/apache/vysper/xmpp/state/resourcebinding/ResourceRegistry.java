package org.apache.vysper.xmpp.state.resourcebinding;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.server.SessionContext;

import java.util.List;

/**
 */
public interface ResourceRegistry {
    SessionContext getSessionContext(String resourceId);

    boolean setResourceState(String resourceId, ResourceState state);

    ResourceState getResourceState(String resourceId);

    List<String> getInterestedResources(Entity entity);

    long getSessionCount();

    String getUniqueResourceForSession(SessionContext sessionContext);

    List<SessionContext> getSessions(Entity entity);

    List<SessionContext> getSessions(Entity entity, Integer prioThreshold);

    List<SessionContext> getHighestPrioSessions(Entity entity, Integer prioThreshold);

    void setResourcePriority(String resourceId, int priority);

    List<String> getAvailableResources(Entity entity);

    String bindSession(SessionContext sessionContext);

    boolean unbindResource(String resourceId);

    void unbindSession(SessionContext unbindingSessionContext);

    List<String> getBoundResources(Entity entity);

    List<String> getBoundResources(Entity entity, boolean considerBareID);

    List<String> getResourcesForSession(SessionContext sessionContext);
}
