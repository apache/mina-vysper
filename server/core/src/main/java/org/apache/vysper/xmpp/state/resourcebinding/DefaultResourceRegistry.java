/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xmpp.state.resourcebinding;

import static org.apache.vysper.xmpp.state.resourcebinding.ResourceState.AVAILABLE;
import static org.apache.vysper.xmpp.state.resourcebinding.ResourceState.AVAILABLE_INTERESTED;
import static org.apache.vysper.xmpp.state.resourcebinding.ResourceState.CONNECTED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.InternalSessionContext;
import org.apache.vysper.xmpp.uuid.JVMBuiltinUUIDGenerator;
import org.apache.vysper.xmpp.uuid.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * assigns and holds resource ids and their related session
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultResourceRegistry implements InternalResourceRegistry {

    final Logger logger = LoggerFactory.getLogger(DefaultResourceRegistry.class);

    private static class SessionData {

        private final InternalSessionContext context;

        private ResourceState state;

        private Integer priority;

        SessionData(InternalSessionContext context, ResourceState status, Integer priority) {
            this.context = context;
            this.state = status;
            this.priority = priority == null ? 0 : priority;
        }

    }

    private UUIDGenerator resourceIdGenerator = new JVMBuiltinUUIDGenerator();

    /**
     * maps resource id to session. note: two resources may point to the same session, but often this
     * is a 1:1 relationship
     */
    protected final Map<String, SessionData> boundResources = new HashMap<String, SessionData>();

    /**
     * an entity's list of resources
     * maps bare JID to all its bound resources. the list of resource ids might not be emtpy, and if there
     * is more than one id, the list usually spans more than 1 session
     */
    protected final Map<Entity, List<String>> entityResources = new HashMap<Entity, List<String>>();

    /**
     * a session's list of resources
     * maps a session to all the resource ids bound to it.
     */
    protected final Map<SessionContext, List<String>> sessionResources = new HashMap<SessionContext, List<String>>();

    /**
     * allocates new resource ID for the given session and binds it to the session
     * @param sessionContext
     * @return newly allocated resource id
     */
    public String bindSession(InternalSessionContext sessionContext) {
        if (sessionContext == null) {
            throw new IllegalArgumentException("session context cannot be NULL");
        }
        if (sessionContext.getInitiatingEntity() == null) {
            throw new IllegalStateException("session context must have a initiating entity set");
        }
        String resourceId = resourceIdGenerator.create();

        synchronized (boundResources) {
            synchronized (entityResources) {
                synchronized (sessionResources) {
                    // record session for the resource id
                    boundResources.put(resourceId, new SessionData(sessionContext, CONNECTED, 0));

                    Entity initiatingEntity = sessionContext.getInitiatingEntity();
                    List<String> resourceForEntityList = getResourceList(initiatingEntity);
                    if (resourceForEntityList == null) {
                        resourceForEntityList = new ArrayList<String>(1);
                        entityResources.put(getBareEntity(initiatingEntity), resourceForEntityList);
                    }
                    resourceForEntityList.add(resourceId);
                    logger.info("added resource no. " + resourceForEntityList.size() + " to entity {} <- {}",
                            initiatingEntity.getFullQualifiedName(), resourceId);

                    List<String> resourcesForSessionList = sessionResources.get(sessionContext);
                    if (resourcesForSessionList == null) {
                        resourcesForSessionList = new ArrayList<String>(1);
                        sessionResources.put(sessionContext, resourcesForSessionList);
                    }
                    resourcesForSessionList.add(resourceId);
                    logger.info("added resource no. " + resourcesForSessionList.size() + " to session {} <- {}",
                            sessionContext.getSessionId(), resourceId);
                }
            }
        }

        return resourceId;
    }

    /**
     * not as commonly used as #unbindSession, this method unbinds only one of multiple resource ids for the _same_
     * session. In XMPP, this is done by sending a stanza like
     * <iq id='unbind_1' type='set'><unbind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>
     * <resource>resourceId</resource>
     * </unbind></iq>
     * @param resourceId
     */
    public boolean unbindResource(String resourceId) {
        boolean noResourceRemainsForSession;
        synchronized (boundResources) {
            synchronized (entityResources) {
                synchronized (sessionResources) {
                    SessionContext sessionContext = getSessionContext(resourceId);

                    // remove from entity's list of resources
                    List<String> resourceListForEntity = getResourceList(sessionContext.getInitiatingEntity());
                    if (resourceListForEntity != null) {
                        resourceListForEntity.remove(resourceId);
                        if (resourceListForEntity.isEmpty()) {
                            entityResources.remove(sessionContext.getInitiatingEntity());
                        }
                    }

                    // remove from session's list of resources
                    List<String> resourceListForSession = sessionResources.get(sessionContext);
                    resourceListForSession.remove(resourceId);
                    noResourceRemainsForSession = resourceListForSession.isEmpty();
                    if (noResourceRemainsForSession)
                        sessionResources.remove(sessionContext);

                    // remove from overall list of bound resource
                    boundResources.remove(resourceId);
                }
            }
        }
        return noResourceRemainsForSession;
    }

    /**
     * unbinds a complete session, together with all its bound resources. this is typically done when a XMPP session
     * end because the client sends a </stream:stream> or the connection is cut.
     * @param unbindingSessionContext sessionContext to be unbound
     */
    public void unbindSession(SessionContext unbindingSessionContext) {
        if (unbindingSessionContext == null)
            return;

        synchronized (boundResources) {
            synchronized (entityResources) {
                synchronized (sessionResources) {
                    // collect all remove candidates
                    List<String> removeResourceIds = getResourcesForSessionInternal(unbindingSessionContext);

                    // actually remove from bound resources
                    for (String removeResourceId : removeResourceIds) {
                        boundResources.remove(removeResourceId);
                    }

                    // actually remove from entity map
                    List<String> resourceList = getResourceList(unbindingSessionContext.getInitiatingEntity());
                    if (resourceList != null) {
                        resourceList.removeAll(removeResourceIds);
                    }

                    // actually remove from session map
                    sessionResources.remove(unbindingSessionContext);
                }
            }
        }
    }

    /**
     * retrieves the one and only bound resource for a given session.
     * @param sessionContext
     * @return null, if a unique resource cannot be determined (there is more or less than 1), the resource id otherwise
     */
    public String getUniqueResourceForSession(SessionContext sessionContext) {
        List<String> list = getResourcesForSessionInternal(sessionContext);
        if (list != null && list.size() == 1)
            return list.get(0);
        return null;
    }

    public List<String> getResourcesForSession(SessionContext sessionContext) {
        return Collections.unmodifiableList(getResourcesForSessionInternal(sessionContext));
    }

    /*package*/List<String> getResourcesForSessionInternal(SessionContext sessionContext) {
        if (sessionContext == null)
            return null;

        List<String> resourceList = sessionResources.get(sessionContext);
        if (resourceList == null)
            resourceList = Collections.emptyList();
        return resourceList;
    }

    public InternalSessionContext getSessionContext(String resourceId) {
        SessionData data = boundResources.get(resourceId);
        if (data == null)
            return null;
        return data.context;
    }

    private Entity getBareEntity(Entity entity) {
        return entity == null ? null : entity.getBareJID();
    }

    /**
     * @param entity
     * @return all resources bound to this entity modulo the entity's resource
     *         (if given)
     */
    private List<String> getResourceList(Entity entity) {
        return entityResources.get(getBareEntity(entity));
    }

    /**
     * retrieve IDs of all bound resources for this entity
     */
    public List<String> getBoundResources(Entity entity) {
        return getBoundResources(entity, true);
    }

    /**
     * retrieve IDs of all bound resources for this entity
     */
    public List<String> getBoundResources(Entity entity, boolean considerBareID) {
        // all resources for the entity
        List<String> resourceList = getResourceList(entity);
        if (resourceList == null)
            return Collections.emptyList();

        // if resource should not be considered, return all resources
        if (considerBareID || entity.getResource() == null)
            return Collections.unmodifiableList(resourceList);
        // resource not contained, result is empty
        if (!resourceList.contains(entity.getResource())) {
            return Collections.emptyList();
        }
        // do we have a bound entity and want only their resource returned?
        return Collections.singletonList(entity.getResource());
    }

    /**
     * retrieves all sessions handling this entity. note: if given entity is not a bare JID, it will return only the
     * session for the JID's resource part. if it's a bare JID, it will return all session for the JID.
     * @param entity
     */
    public List<InternalSessionContext> getSessions(Entity entity) {
        List<InternalSessionContext> sessionContexts = new ArrayList<>();

        List<String> boundResources = getBoundResources(entity, false);
        for (String resourceId : boundResources) {
            sessionContexts.add(getSessionContext(resourceId));
        }

        return sessionContexts;
    }

    /**
     * retrieves sessions with same or above threshold
     *
     * @param entity all session for the bare jid will be considered.
     * @param prioThreshold only resources will be returned having same or higher priority. a common value
     * for the threshold is 0 (zero), which is also the default when param is NULL.
     * @return returns the sessions matching the given JID (bare) with same or higher priority
     */
    public List<InternalSessionContext> getSessions(Entity entity, Integer prioThreshold) {
        if (prioThreshold == null)
            prioThreshold = 0;
        List<InternalSessionContext> results = new ArrayList<>();

        List<String> boundResourceIds = getBoundResources(entity, true);
        for (String resourceId : boundResourceIds) {
            SessionData sessionData = boundResources.get(resourceId);
            if (sessionData == null)
                continue;

            if (sessionData.priority >= prioThreshold) {
                results.add(sessionData.context);
            }
        }
        return results;
    }

    /**
     * number of active bare ids (# of users, regardless whether they have one or more connected sessions)
     * @return
     */
    public long getSessionCount() {
        return entityResources.size();
    }

    /**
     * retrieves the highest prioritized session(s) for this entity.
     * 
     * @param entity if this is not a bare JID, only the session for the JID's resource part will be returned, without
     * looking at other sessions for the resource's bare JID. otherwise, in case of a full JID, it will return the
     * highest prioritized sessions.
     * @param prioThreshold if not NULL, only resources will be returned having same or higher priority. a common value
     * for the threshold is 0 (zero).
     * @return for a bare JID, it will return the highest prioritized sessions. for a full JID, it will return the
     * related session.
     */
    public List<InternalSessionContext> getHighestPrioSessions(Entity entity, Integer prioThreshold) {
        Integer currentPrio = prioThreshold == null ? Integer.MIN_VALUE : prioThreshold;
        List<InternalSessionContext> results = new ArrayList<>();

        boolean isResourceSet = entity.isResourceSet();

        List<String> boundResourceIds = getBoundResources(entity, false);
        for (String resourceId : boundResourceIds) {
            SessionData sessionData = boundResources.get(resourceId);
            if (sessionData == null)
                continue;

            if (isResourceSet) {
                // if resource id matches, there can only be one result
                // this overrides even parameter prio threshold
                results.clear();
                results.add(sessionData.context);
                return results;
            }

            if (sessionData.priority > currentPrio) {
                results.clear(); // discard all accumulated lower prio sessions
                currentPrio = sessionData.priority;
                results.add(sessionData.context);
            } else if (sessionData.priority.intValue() == currentPrio.intValue()) {
                results.add(sessionData.context);
            }
        }

        return results;
    }

    /**
     * Sets the {@link ResourceState} for the given resource.
     *
     * @param resourceId
     *            the resource identifier
     * @param state
     *            the {@link ResourceState} to set
     * @return true iff the state has effectively changed
     */
    public boolean setResourceState(String resourceId, ResourceState state) {
        SessionData data = boundResources.get(resourceId);
        if (data == null) {
            throw new IllegalArgumentException("resource not registered: " + resourceId);
        }
        synchronized (data) {
            boolean result = data.state != state;
            data.state = state;
            return result;
        }
    }

    /**
     * Gets the {@link ResourceState} of the given resource.
     *
     * @param resourceId
     *            the resource identifier
     * @return the {@link ResourceState}
     */
    public ResourceState getResourceState(String resourceId) {
        if (resourceId == null)
            return null;
        SessionData data = boundResources.get(resourceId);
        if (data == null)
            return null;
        return data.state;
    }

    public void setResourcePriority(String resourceId, int priority) {
        if (resourceId == null)
            return;
        SessionData data = boundResources.get(resourceId);
        if (data == null)
            return;
        data.priority = priority;
    }

    public List<String> getInterestedResources(Entity entity) {
        List<String> resources = getResourceList(entity);
        List<String> result = new ArrayList<String>();
        if (resources == null) return result;
        
        for (String resource : resources) {
            ResourceState resourceState = getResourceState(resource);
            if (ResourceState.isInterested(resourceState))
                result.add(resource);
        }
        return result;
    }

    /**
     * resources which are available or even interested - an higher form of available.
     * @see org.apache.vysper.xmpp.state.resourcebinding.ResourceState
     */
    public List<String> getAvailableResources(Entity entity) {
        List<String> resources = getResourceList(entity);
        List<String> result = new ArrayList<String>();
        if (resources == null) return result;

        for (String resource : resources) {
            ResourceState resourceState = getResourceState(resource);
            if (resourceState == AVAILABLE || resourceState == AVAILABLE_INTERESTED) {
                result.add(resource);
            }
        }
        return result;
    }
}
