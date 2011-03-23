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
package org.apache.vysper.xmpp.extension.xep0065_socks;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.session.IoSession;

/**
 * A pair of SOCKS5 sessions, paired by the hash provided as the domain name
 * according to XEP-0065.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Socks5Pair {
    
    private String hash;
    private Socks5ConnectionsRegistry registry;
    private IoSession requester;
    private IoSession target;
    private AtomicBoolean activated = new AtomicBoolean(false);
    
    public Socks5Pair(IoSession target, Socks5ConnectionsRegistry registry, String hash) {
        this.target = target;
        this.registry = registry;
        this.hash = hash;
    }

    public synchronized IoSession getOther(IoSession session) {
        if(requester.equals(session)) {
            return target;
        } else {
            return requester;
        }
    }
    
    public synchronized IoSession getRequester() {
        return requester;
    }

    public synchronized void setRequester(IoSession requester) {
        this.requester = requester;
    }
    
    public IoSession getTarget() {
        return target;
    }

    public boolean isActivated() {
        return activated.get();
    }
    
    public void activate() {
        activated.set(true);
    }
    
    public void close() {
        if(requester != null) {
            requester.close(false);
        }
        if(target != null) {
            target.close(false);
        }
        
        registry.close(hash);
    }
}