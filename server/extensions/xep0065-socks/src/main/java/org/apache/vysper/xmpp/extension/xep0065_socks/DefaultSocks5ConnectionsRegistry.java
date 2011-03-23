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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.IoSession;

/**
 * The default implementation of {@link Socks5ConnectionsRegistry}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class DefaultSocks5ConnectionsRegistry implements Socks5ConnectionsRegistry {
    
    private Map<String, Socks5Pair> pairs = new ConcurrentHashMap<String, Socks5Pair>();

    /**
     * {@inheritDoc}
     */
    public Socks5Pair register(String hash, IoSession session) {
        Socks5Pair pair = pairs.get(hash);
        if(pair == null) {
            // new pair, this must be the target
            pair = new Socks5Pair(session, this, hash);
            pairs.put(hash, pair);
        } else {
            // update pair
            pair.setRequester(session);
        }
        
        return pair;
    }
    
    public IoSession getTarget(String hash) {
        Socks5Pair pair = pairs.get(hash);
        if(pair != null) {
            return pair.getTarget();
        } else {
            return null;
        }
    }
    
    public IoSession getRequester(String hash) {
        Socks5Pair pair = pairs.get(hash);
        if(pair != null) {
            return pair.getRequester();
        } else {
            return null;
        }
    }
    
    /**
     * Returns true if a pair has been activated. Will return
     * false if a hash is unknown to the registry.
     * @param hash
     * @return
     */
    public boolean isActivated(String hash) {
        Socks5Pair pair = pairs.get(hash);
        if(pair != null) {
            return pair.isActivated();
        } else {
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean activate(String hash) {
        Socks5Pair pair = pairs.get(hash);
        if(pair != null && pair.getRequester() != null) {
            pair.activate();
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void close(String hash) {
        if(pairs.containsKey(hash)) {
            pairs.remove(hash);
        }
    }
}