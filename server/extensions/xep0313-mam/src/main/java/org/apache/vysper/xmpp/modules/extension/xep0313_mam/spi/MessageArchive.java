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

/**
 * <a href=
 * "https://xmpp.org/extensions/xep-0313.html#business-storeret">Storage and
 * Retrieval Rules</a>
 * 
 * @author Réda Housni Alaoui
 */
public interface MessageArchive {

    /**
     * At a minimum, the server MUST store the <body> elements of a stanza. It is
     * suggested that other elements that are used in a given deployment to
     * supplement conversations (e.g. XHTML-IM payloads) are also stored. Other
     * elements MAY be stored.
     */
    ArchivedMessage archive(Message message);

    ArchivedMessages fetchSortedByOldestFirst(MessageFilter messageFilter, MessagePageRequest pageRequest);

    ArchivedMessages fetchLastPageSortedByOldestFirst(MessageFilter messageFilter, long pageSize);

}
