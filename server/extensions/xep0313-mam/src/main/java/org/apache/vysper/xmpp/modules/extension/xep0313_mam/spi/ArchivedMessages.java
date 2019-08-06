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

import java.util.List;
import java.util.Optional;

/**
 * @author Réda Housni Alaoui
 */
public interface ArchivedMessages {

    List<ArchivedMessage> list();

    boolean isEmpty();

    /**
     * When the results returned by the server are complete (that is: when they have
     * not been limited by the maximum size of the result page (either as specified
     * or enforced by the server)), the server MUST include a 'complete' attribute
     * on the <fin> element, with a value of 'true'; this informs the client that it
     * doesn't need to perform further paging to retreive the requested data. If it
     * is not the last page of the result set, the server MUST either omit the
     * 'complete' attribute, or give it a value of 'false'.
     */
    boolean isComplete();

    /**
     * This integer specifies the position within the full set (which MAY be
     * approximate) of the first message in the page. If that message is the first
     * in the full set, then the index SHOULD be '0'. If the last message in the
     * page is the last message in the full set, then the value SHOULD be the
     * specified count minus the number of messages in the last page.
     */
    Optional<Long> firstMessageIndex();

    /**
     * @return The total number of messages that could be retrieved by paging
     *         through the pages.
     */
    Optional<Long> totalNumberOfMessages();

}
