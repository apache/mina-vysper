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

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * <a href="https://xmpp.org/extensions/xep-0313.html#filter-time">Filtering by
 * time received</a>
 * 
 * @author Réda Housni Alaoui
 */
public interface DateTimeFilter {

    /**
     * The 'start' field is used to filter out messages before a certain date/time.
     * If specified, a server MUST only return messages whose timestamp is equal to
     * or later than the given timestamp.
     */
    Optional<ZonedDateTime> start();

    /**
     * The 'end' field is used to exclude from the results messages
     * after a certain point in time. If specified, a server MUST only return
     * messages whose timestamp is equal to or earlier than the timestamp given in
     * the 'end' field.
     */
    Optional<ZonedDateTime> end();

}
