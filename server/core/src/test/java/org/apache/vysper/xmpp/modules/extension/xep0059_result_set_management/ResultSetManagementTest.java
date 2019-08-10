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
package org.apache.vysper.xmpp.modules.extension.xep0059_result_set_management;

import static org.junit.Assert.assertEquals;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
public class ResultSetManagementTest {

    @Test
    public void parse() throws XMLSemanticError {
        Set tested = Set.builder().after("after-value").before("before-value").count(5)
                .index(10).last("last-value").max(15).startFirst().index(20).value("first-value").endFirst().build();

        assertEquals("after-value", tested.getAfter().get());
        assertEquals("before-value", tested.getBefore().get());
        assertEquals(5, tested.getCount().getAsInt());
        assertEquals(10, tested.getIndex().getAsInt());
        assertEquals("last-value", tested.getLast().get());
        assertEquals(15, tested.getMax().getAsInt());

        Set.First first = tested.getFirst().get();
        assertEquals("first-value", first.getValue());
        assertEquals(20, first.getIndex().getAsInt());
    }

}