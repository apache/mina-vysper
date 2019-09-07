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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.user;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.DefaultUserArchiveBehaviour;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.UserMessageArchivePreferences;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleUserMessageArchivePreferences implements UserMessageArchivePreferences {

    private final DefaultUserArchiveBehaviour defaultBehaviour;

    private final Set<Entity> alwaysArchivedToOrFromJids;

    private final Set<Entity> neverArchivedToOrFromJids;

    public SimpleUserMessageArchivePreferences() {
        this(DefaultUserArchiveBehaviour.ALWAYS, Collections.emptySet(), Collections.emptySet());
    }

    public SimpleUserMessageArchivePreferences(DefaultUserArchiveBehaviour defaultBehaviour,
            Set<Entity> alwaysArchivedToOrFromJids, Set<Entity> neverArchivedToOrFromJids) {
        this.defaultBehaviour = requireNonNull(defaultBehaviour);
        this.alwaysArchivedToOrFromJids = new HashSet<>(alwaysArchivedToOrFromJids);
        this.neverArchivedToOrFromJids = new HashSet<>(neverArchivedToOrFromJids);
    }

    @Override
    public DefaultUserArchiveBehaviour getDefaultBehaviour() {
        return defaultBehaviour;
    }

    @Override
    public Set<Entity> getAlwaysArchivedToOrFromJids() {
        return alwaysArchivedToOrFromJids;
    }

    @Override
    public Set<Entity> getNeverArchivedToOrFromJids() {
        return neverArchivedToOrFromJids;
    }
}
