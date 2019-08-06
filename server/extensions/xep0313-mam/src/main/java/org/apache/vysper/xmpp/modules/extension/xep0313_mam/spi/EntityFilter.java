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

import org.apache.vysper.xmpp.addressing.Entity;

/**
 * @author Réda Housni Alaoui
 */
public interface EntityFilter {

    enum Type {
        /**
         * The message 'to' or 'from' must match the provided entity
         */
        TO_OR_FROM,
        /**
         * The message 'to' and 'from' must match the provided entity
         */
        TO_AND_FROM;
    }

    /**
     * @return The entity to filter by
     */
    Entity entity();

    /**
     * @return The type of filtering
     */
    Type type();

    /**
     * @return True to ignore the filtering entity, the message 'to' and the message
     *         'from' <b>resource part</b>.
     */
    boolean ignoreResource();

}
