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

package org.apache.vysper.xmpp.addressing;

import org.apache.vysper.xmpp.addressing.stringprep.NodePrep;
import org.apache.vysper.xmpp.addressing.stringprep.ResourcePrep;
import org.apache.vysper.xmpp.addressing.stringprep.StringPrepViolationException;

/**
 * {@link Entity} implementation. provides consersion helper method {@link #parse(String)} to create Entity from String
 * represenation.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class EntityImpl implements Entity {

    public static final String CHAR_AT = "@";

    public static final String CHAR_SLASH = "/";

    private String node;

    private String domain;

    private String resource;

    protected String fullyQualifiedCached = null;

    protected Entity bareEntityCached = null;

    public static EntityImpl parse(String entity) throws EntityFormatException {
        String node = null;
        String domain;
        String resource = null;
        if (entity == null)
            throw new EntityFormatException("entity must not be NULL");

        if (entity.contains(CHAR_AT)) {
            String[] parts = entity.split(CHAR_AT);
            if (parts.length != 2)
                throw new EntityFormatException("entity must be of format node@domain/resource");
            node = parts[0];
            node = NodePrep.prepare(node);
            entity = parts[1];
        }
        domain = entity;
        if (entity.contains(CHAR_SLASH)) {
            int indexOfSlash = entity.indexOf(CHAR_SLASH);
            domain = entity.substring(0, indexOfSlash);
            resource = entity.substring(indexOfSlash + 1);
            resource = ResourcePrep.prepare(resource);
        }
        return new EntityImpl(node, domain, resource, true);
    }

    /**
     * Parse entities, throwing {@link IllegalArgumentException} on format errors
     * @param entity
     * @return
     */
    public static EntityImpl parseUnchecked(String entity) {
        try {
            return EntityImpl.parse(entity);
        } catch (EntityFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private EntityImpl(String node, String domain, String resource, boolean prepped) {
        try {
            this.node = (!prepped && node != null) ? NodePrep.prepare(node): node;
            this.domain = domain;
            this.resource = (!prepped && resource != null) ? ResourcePrep.prepare(resource) : resource;
        } catch (StringPrepViolationException e) {
            throw new RuntimeException(e);
        }
    }

    public EntityImpl(String node, String domain, String resource) {
        this(node, domain, resource, false);
    }

    public EntityImpl(Entity bareId, String resource) {
        this (bareId.getNode(), bareId.getDomain(), resource);
    }

    public String getNode() {
        return node;
    }

    public String getDomain() {
        return domain;
    }

    public String getResource() {
        return resource;
    }

    public String getFullQualifiedName() {
        if (fullyQualifiedCached == null)
            fullyQualifiedCached = buildEntityString(node, domain, resource);
        return fullyQualifiedCached;
    }

    private String buildEntityString(String node, String domain, String resource) {
        StringBuilder buffer = new StringBuilder();
        if (isNodeSet())
            buffer.append(node).append(CHAR_AT);
        buffer.append(domain);
        if (isResourceSet())
            buffer.append(CHAR_SLASH).append(resource);
        return buffer.toString();
    }

    public Entity getBareJID() {
        if (!isResourceSet())
            return this; // this _is_ a bare id
        if (bareEntityCached == null)
            bareEntityCached = new EntityImpl(node, domain, null);
        return bareEntityCached;
    }

    public String getCanonicalizedName() {
        return null;
    }

    public boolean isNodeSet() {
        return node != null && !"".equals(node);
    }

    public boolean isResourceSet() {
        return resource != null && !"".equals(resource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof Entity))
            return false;

        final Entity that = (Entity) o;

        if (!checkDomainsEqual(that))
            return false;
        if (isNodeSet() != that.isNodeSet())
            return false;
        if (isNodeSet()) {
            if (node != null ? !node.equals(that.getNode()) : that.getNode() != null)
                return false;
        }
        if (isResourceSet() != that.isResourceSet())
            return false;
        if (isResourceSet()) {
            if (resource != null ? !resource.equals(that.getResource()) : that.getResource() != null)
                return false;
        }

        return true;
    }

    public boolean checkDomainsEqual(Entity that) {
        if (domain == null) return that.getDomain() == null;
        return domain.toLowerCase().equals(that.getDomain().toLowerCase());
    }

    @Override
    public int hashCode() {
        int result;
        result = (node != null ? node.hashCode() : 0);
        result = 29 * result + (domain != null ? domain.hashCode() : 0);
        result = 29 * result + (resource != null ? resource.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getFullQualifiedName();
    }
}
