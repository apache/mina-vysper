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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.modules.extension.xep0059_result_set_management.Set;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class QuerySetTest {

    @Test
    public void mainCase() throws XMLSemanticError {
        Set set = Set.builder().max(10L).after("first").before("last").build();
        QuerySet tested = new QuerySet(set);
        assertEquals(10L, (long) tested.pageSize().orElse(0L));
        assertEquals("first", tested.firstMessageId().orElse(null));
        assertEquals("last", tested.lastMessageId().orElse(null));
    }

    @Test
    public void maxAndFirstMessageIdBlankIsALastPageQuery() throws XMLSemanticError {
        Set set = Set.builder().max(10L).before("").build();
        QuerySet tested = new QuerySet(set);
        assertTrue(tested.lastPage());
    }

    @Test
    public void maxAndFirstMessageIdBlankAndLastMessageIdNotBlankIsNotALastPageQuery() throws XMLSemanticError {
        Set set = Set.builder().max(10L).after("first").before("").build();
        QuerySet tested = new QuerySet(set);
        assertFalse(tested.lastPage());
    }

}