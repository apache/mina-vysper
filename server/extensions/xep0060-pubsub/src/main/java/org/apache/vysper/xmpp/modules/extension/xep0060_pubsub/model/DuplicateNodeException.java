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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model;

/**
 * This exception is used to deal with the case that a new node should
 * be created with a JID already taken by some other node.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class DuplicateNodeException extends Exception {

    private static final long serialVersionUID = 4689474856848508356L;

    /**
     * Initializes the exception with a free-form string.
     * 
     * @param description further details about the exception.
     */
    public DuplicateNodeException(String description) {
        super(description);
    }
}
