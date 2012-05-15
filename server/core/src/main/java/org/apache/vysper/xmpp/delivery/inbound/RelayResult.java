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
package org.apache.vysper.xmpp.delivery.inbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;

public class RelayResult {
    private List<DeliveryException> processingErrors = new ArrayList<DeliveryException>();
    protected boolean processed = false;

    public RelayResult() {
        // empty
    }

    public RelayResult(DeliveryException processingError) {
        addProcessingError(processingError);
        setProcessed();
    }

    public RelayResult setProcessed() {
        processed = true;
        return this;
    }
    
    public void addProcessingError(DeliveryException processingError) {
        processingErrors.add(processingError);
    }

    public List<DeliveryException> getProcessingErrors() {
        return Collections.unmodifiableList(processingErrors);
    }

    public boolean hasProcessingErrors() {
        return processingErrors.size() > 0;
    }
}