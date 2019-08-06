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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi;

import static java.util.Objects.requireNonNull;

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleEntityFilter implements EntityFilter {

    private final Entity entity;

    private final Type type;

    private final boolean ignoreResource;

    public SimpleEntityFilter(Entity entity, Type type, boolean ignoreResource) {
        this.entity = requireNonNull(entity);
        this.type = requireNonNull(type);
        this.ignoreResource = ignoreResource;
    }

    @Override
    public Entity entity() {
        return entity;
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
