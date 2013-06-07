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

package org.apache.vysper.xmpp.state.resourcebinding;

/**
 * This enumeration represents the status of a bound resource.
 *
 * see http://www.xmpp.org/internet-drafts/draft-saintandre-rfc3921bis-04.html#roster-login
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 *
 */
public enum ResourceState {
    /**
     * A resource is connected if resource binding was successful.
     */
    CONNECTED,
    /**
     * A conntected resource has requested the
     * entity's roster without sending initial presence first
     */
    CONNECTED_INTERESTED,
    /**
    * A connected resource is considered "available" after successfully sending
      * its initial presence, it has not requested the roster yet
      */
    AVAILABLE,
    /**
     * An available resource is considered "interested" after requesting the
     * entity's roster.
     */
    AVAILABLE_INTERESTED,
    /**
      * A resource is no longer "available"
      */
    UNAVAILABLE;

    public static boolean isInterested(ResourceState resourceState) {
        return resourceState == CONNECTED_INTERESTED || resourceState == AVAILABLE_INTERESTED;
    }

    public static boolean isAvailable(ResourceState resourceState) {
        return resourceState == AVAILABLE || resourceState == AVAILABLE_INTERESTED;
    }

    /**
     * depending on the inState, moves the state to AVAILABLE, or directly keeps/promotes to INTERESTED
     * @param inState the current state you want to change
     * @return new state
     */
    public static ResourceState makeAvailable(ResourceState inState) {
        if (inState == null || !isInterested(inState))
            return AVAILABLE;
        return AVAILABLE_INTERESTED;
    }

    public static ResourceState makeInterested(ResourceState inState) {
        if (inState == null) return null; // cannot transfer to 'interested'
        switch (inState) {
            case CONNECTED:
            case CONNECTED_INTERESTED:
                return CONNECTED_INTERESTED;
            case AVAILABLE:
            case AVAILABLE_INTERESTED:
                return AVAILABLE_INTERESTED;
            case UNAVAILABLE:
                return UNAVAILABLE;
            default:
                throw new IllegalStateException("unknown ResourceState in makeInterested(): " + inState);
        }
    }
}
