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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.feature;

import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * Encapsulates a feature of the XEP0060 specification.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubsubFeature {

    protected String name;

    protected String description;

    protected PubsubFeatureLevel level;

    protected String xep0060chapter;

    /**
     * Creates a new Pubsub Feature object with the given values.
     * 
     * @param name
     * @param description
     * @param level
     * @param xep0060chapter
     */
    public PubsubFeature(String name, String description, PubsubFeatureLevel level, String xep0060chapter) {
        this.name = name;
        this.description = description;
        this.level = level;
        this.xep0060chapter = xep0060chapter;
    }

    /**
     * Returns a string representation of the features (complete with the pubsub namespace).
     */
    @Override
    public String toString() {
        return NamespaceURIs.XEP0060_PUBSUB + "#" + this.name;
    }

    /**
     * @return creates a service discovery feature object out of this pubsub feature.
     */
    public Feature getFeature() {
        return new Feature(this.toString());
    }
}
