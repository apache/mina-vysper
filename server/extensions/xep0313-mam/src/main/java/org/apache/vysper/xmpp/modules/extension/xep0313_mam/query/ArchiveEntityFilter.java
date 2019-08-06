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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.query;

import static java.util.Objects.requireNonNull;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.EntityFilter;

/**
 * @author Réda Housni Alaoui
 */
public class ArchiveEntityFilter implements EntityFilter {

    private final Entity with;

    private final Type type;

    private final boolean ignoreResource;

    public ArchiveEntityFilter(Entity archiveId, Entity with) {
        this.with = requireNonNull(with);
        if (archiveId.equals(with)) {
            // If the 'with' field's value is the bare JID of the archive, the server must
            // only return results where both
            // the 'to' and 'from' match the bare JID (either as bare or by ignoring the
            // resource), as otherwise every
            // message in the archive would match
            this.type = Type.TO_AND_FROM;
            this.ignoreResource = true;
        } else if (!with.isResourceSet()) {
            // If (and only if) the supplied JID is a bare JID (i.e. no resource is
            // present),
            // then the server SHOULD return messages if their bare to/from address for a
            // user archive would match it.
            this.type = Type.TO_OR_FROM;
            this.ignoreResource = true;
        } else {
            // A message in a user's archive matches if the JID matches either the to or
            // from of the message.
            this.type = Type.TO_OR_FROM;
            this.ignoreResource = false;
        }
    }

    @Override
    public Entity entity() {
        return with;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public boolean ignoreResource() {
        return ignoreResource;
    }
    
    
}
