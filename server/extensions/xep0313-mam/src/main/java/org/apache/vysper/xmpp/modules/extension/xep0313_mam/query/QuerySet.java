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

import static java.util.Optional.ofNullable;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.modules.extension.xep0059_result_set_management.Set;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessagePageRequest;

/**
 * @author Réda Housni Alaoui
 */
public class QuerySet implements MessagePageRequest {

    private final Long max;

    private final String after;

    private final String before;

    public QuerySet(Set set) throws XMLSemanticError {
        this.max = set.getMax().orElse(null);
        this.after = set.getAfter().orElse(null);
        this.before = set.getBefore().orElse(null);
    }

    public static QuerySet empty() throws XMLSemanticError {
        return new QuerySet(Set.empty());
    }

    @Override
    public Optional<Long> pageSize() {
        return ofNullable(max);
    }

    @Override
    public Optional<String> firstMessageId() {
        return ofNullable(after);
    }

    @Override
    public Optional<String> lastMessageId() {
        return ofNullable(before);
    }

    /**
     * <a href="https://xmpp.org/extensions/xep-0059.html#last">Requesting the Last
     * Page in a Result Set</a>
     *
     * The requesting entity MAY ask for the last page in a result set by including
     * in its request an empty <before/> element, and the maximum number of items to
     * return.
     */
    public boolean lastPage() {
        return max != null && after == null && ofNullable(before).filter(StringUtils::isBlank).isPresent();
    }
}
