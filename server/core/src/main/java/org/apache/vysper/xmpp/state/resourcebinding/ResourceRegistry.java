package org.apache.vysper.xmpp.state.resourcebinding;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;

import java.util.List;

/**
 */
public interface ResourceRegistry {

    boolean setResourceState(String resourceId, ResourceState state);

    ResourceState getResourceState(String resourceId);

    List<String> getInterestedResources(Entity entity);

    long getSessionCount();

    String getUniqueResourceForSession(SessionContext sessionContext);

    void setResourcePriority(String resourceId, int priority);

    List<String> getAvailableResources(Entity entity);

    String bindSession(InternalSessionContext sessionContext);

    boolean unbindResource(String resourceId);

    void unbindSession(SessionContext unbindingSessionContext);

    List<String> getBoundResources(Entity entity);

    List<String> getBoundResources(Entity entity, boolean considerBareID);

    List<String> getResourcesForSession(SessionContext sessionContext);
}
