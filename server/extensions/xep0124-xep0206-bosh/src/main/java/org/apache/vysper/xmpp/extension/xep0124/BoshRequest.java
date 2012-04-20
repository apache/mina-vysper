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
package org.apache.vysper.xmpp.extension.xep0124;

import javax.servlet.http.HttpServletRequest;

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * Wraps an HTTP request with its XML BOSH body.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class BoshRequest implements Comparable<BoshRequest> {

    private final HttpServletRequest httpServletRequest;

    private final Stanza body;

    private final Long rid;
    
    private final long timestamp;

    public BoshRequest(HttpServletRequest httpServletRequest, Stanza body, Long rid) {
        this.httpServletRequest = httpServletRequest;
        this.body = body;
        this.rid = rid;
        timestamp = System.currentTimeMillis();
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public Stanza getBody() {
        return body;
    }

    public Long getRid() {
        return rid;
    }

    public int compareTo(BoshRequest br) {
        return rid.compareTo(br.rid);
    }

    @Override
    public int hashCode() {
        return rid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BoshRequest other = (BoshRequest) obj;
        if (rid == null) {
            if (other.rid != null) return false;
        } else {
            if (!rid.equals(other.rid)) return false;
        }
        return true;
    }

    public long getTimestamp() {
        return timestamp;
    }

}