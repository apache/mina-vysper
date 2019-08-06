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

import java.util.Optional;

import org.apache.commons.lang.StringUtils;

/**
 * @author Réda Housni Alaoui
 */
public class SimpleMessagePageRequest implements MessagePageRequest {

    private final Long pageSize;

    private final String firstMessageId;

    private final String lastMessageId;

    public SimpleMessagePageRequest(Long pageSize, String firstMessageId, String lastMessageId) {
        this.pageSize = pageSize;
        this.firstMessageId = firstMessageId;
        this.lastMessageId = lastMessageId;
    }

    @Override
    public Optional<Long> pageSize() {
        return Optional.ofNullable(pageSize);
    }

    @Override
    public Optional<String> firstMessageId() {
        return Optional.ofNullable(firstMessageId).filter(StringUtils::isNotBlank);
    }

    @Override
    public Optional<String> lastMessageId() {
        return Optional.ofNullable(lastMessageId).filter(StringUtils::isNotBlank);
    }

}
