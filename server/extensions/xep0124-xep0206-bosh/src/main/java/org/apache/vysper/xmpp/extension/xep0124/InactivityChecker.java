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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.vysper.xmpp.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;

/**
 * Periodically checks the BOSH sessions to see if there are inactive sessions,
 * in this case it will close the inactive sessions.
 * <p>
 * This class efficiently checks the inactive sessions.
 * Supposing that at a specific moment there are N total sessions connected and from these sessions
 * there are M sessions that are inactive (M is lower than or equal to N) then this class will detect the inactive sessions with
 * O(M) time complexity.
 * <p>
 * <b>Note:</b> A modification of the expire (when becoming inactive) time  of a sessions has approximatively 
 * O(log N) time complexity.
 * <p>
 * This class is thread safe.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class InactivityChecker extends Thread {

    private final static Logger LOGGER = LoggerFactory.getLogger(InactivityChecker.class);
    
    protected BoshHandler boshHandler;

    /**
     * reduces the key resolution to 10th of a second
     * @param timestampMillis
     * @return timestampDeci
     */
    public static long convertToKey(long timestampMillis) {
        return timestampMillis / 100;
    }
    
    /*
     * The interval in milliseconds between two consecutive inactivity checks.
     */
    private final int CHECKING_INTERVAL_MILLIS = 10*1000;

    /*
     * Keeps the BOSH sessions sorted according to the time they expire (the key of the map).
     * 
     * The value associated with a key in the map can be a BoshBackedSessionContext
     * or a Set<BoshBackedSessionContext> (in case more than one session expires at the same time).
     */
    private final ListMultimap<Long, BoshBackedSessionContext> sessions = ArrayListMultimap.create();


    public InactivityChecker(BoshHandler boshHandler) {
        this.boshHandler = boshHandler;
        setName(InactivityChecker.class.getSimpleName());
        setDaemon(true);
    }

    /**
     * Updates (or removes) a session expire time.
     * <p>
     * If it is a new session then oldExpireTime will be null and newExpireTime will be the expire time,
     * if the session is removed from the inactivity checker then the newExpireTime will be null and the oldExpireTime
     * will be the latest expire time the session had. If it is an update for an old expire time then oldExpireTime will be
     * the latest expire time and newExpireTime will be the updated expire time.
     * <p>
     * <b>Note:</b> The session should be added to the inactivity checker only when BoshBackedSessionContext#requestsWindow.isEmpty()
     * returns true (as stated in the specification). Also when the BoshBackedSessionContext#requestsWindow.isEmpty() returns
     * false the session needs to be removed from the inactivity checker.
     *  
     * @param session the {@link BoshBackedSessionContext} that the expire time is modified for
     * @param oldExpireTime the latest expire time the session had
     * @param newExpireTime the new updated expire time, (if this is null and the session is watched by the inactivity checker
     * then the session will be removed from the inactivity checker)
     * @return true if the inactivity checker is watching the session (to detect inactivity), false otherwise
     */
    public boolean updateExpireTime(BoshBackedSessionContext session, Long oldExpireTime, Long newExpireTime) {
        boolean ret = session.isWatchedByInactivityChecker();
        if (oldExpireTime == null && newExpireTime == null) {
            return ret;
        }
        Long oldExpireTimeKey = oldExpireTime == null ? null : InactivityChecker.convertToKey(oldExpireTime);
        Long newExpireTimeKey = newExpireTime == null ? null : InactivityChecker.convertToKey(newExpireTime);
        if (newExpireTimeKey != null && newExpireTimeKey.equals(oldExpireTimeKey)) {
            // do not update the key, if it didn't change
            return ret;
        }
        synchronized (sessions) {
            if (oldExpireTimeKey != null) {
                sessions.remove(oldExpireTimeKey, session);
                ret = false;
            }
            if (newExpireTimeKey != null) {
                sessions.put(newExpireTimeKey, session);
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                if (Thread.interrupted()) {
                    break;
                }

                try {
                    Thread.sleep(CHECKING_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    break;
                }

                runWorker();
            } catch (Throwable e) {
                LOGGER.error("inactivity checker exception", e);
            }
        }
        LOGGER.info("inactivity checker watcher thread terminates");
    }

    protected void runWorker() {
        long nowKey = convertToKey(System.currentTimeMillis());

        final HashSet<Long> keys;
        synchronized (sessions) {
            // get all keys
            keys = new HashSet<Long>(sessions.keySet());
        }

        for (Long expireTime : keys) {
            // expire old sessions 
            if (nowKey >= expireTime) {
                final List<BoshBackedSessionContext> inactiveSessions = sessions.removeAll(expireTime);
                for (BoshBackedSessionContext session : inactiveSessions) {
                    LOGGER.info("BOSH session {} reached maximum inactivity period, closing session...", session.getSessionId());
                    try {
                        session.endSession(SessionContext.SessionTerminationCause.CONNECTION_ABORT);
                        final boolean removed = boshHandler.removeSession(session.getSessionId());
                    } catch (Throwable e) {
                        LOGGER.warn("BOSH session {}: error when closing session", e);
                    }
                }
            }
        }
    }

}
