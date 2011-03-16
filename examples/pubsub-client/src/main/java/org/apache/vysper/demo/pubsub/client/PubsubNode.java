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
package org.apache.vysper.demo.pubsub.client;

/**
 * POJ for storing node information.
 *
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubsubNode implements Comparable<PubsubNode> {
    private String node;

    private Boolean subscribed;

    private Boolean ownership;

    public PubsubNode(String node) {
        this.node = node;
        this.subscribed = false;
        this.ownership = false;
    }

    public String getNode() {
        return node;
    }

    public Boolean getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public boolean getOwnership() {
        return ownership;
    }

    public void setOwnership(boolean owner) {
        this.ownership = owner;
    }

    @Override
    public boolean equals(Object obj) {
        return ((PubsubNode) obj).getNode().equals(getNode());
    }

    @Override
    public int hashCode() {
        return getNode().hashCode();
    }

    public int compareTo(PubsubNode o) {
        return this.node.compareTo(o.getNode());
    }
}
