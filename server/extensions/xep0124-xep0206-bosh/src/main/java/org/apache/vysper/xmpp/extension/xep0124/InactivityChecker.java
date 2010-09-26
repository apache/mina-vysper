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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /*
     * The interval in milliseconds between two consecutive inactivity checks.
     */
    private final int CHECKING_INTERVAL = 1000;

    /*
     * Keeps the BOSH sessions sorted according to the time they expire (the key of the map).
     * 
     * The value associated with a key in the map can be a BoshBackedSessionContext
     * or a Set<BoshBackedSessionContext> (in case more than one session expires at the same time).
     */
    private final SortedMap<Long, Object> sessions;

    public InactivityChecker() {
        setName(InactivityChecker.class.getSimpleName());
        setDaemon(true);
        sessions = new TreeMap<Long, Object>();
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean updateExpireTime(BoshBackedSessionContext session, Long oldExpireTime, Long newExpireTime) {
        boolean ret = session.isWatchedByInactivityChecker();
        if ((oldExpireTime == null && newExpireTime == null)
                || (newExpireTime != null && newExpireTime.equals(oldExpireTime))) {
            return ret;
        }
        synchronized (sessions) {
            if (oldExpireTime != null) {
                Object oldValue = sessions.get(oldExpireTime);
                if (oldValue instanceof Set) {
                    ((Set) oldValue).remove(session);
                    if (((Set) oldValue).isEmpty()) {
                        sessions.remove(oldExpireTime);
                    }
                } else if (oldValue != null) {
                    sessions.remove(oldExpireTime);
                }
                ret = false;
            }
            if (newExpireTime != null) {
                Object value = sessions.get(newExpireTime);
                if (value instanceof Set) {
                    ((Set) value).add(session);
                } else if (value == null) {
                    sessions.put(newExpireTime, session);
                } else {
                    Set<BoshBackedSessionContext> set = new HashSet<BoshBackedSessionContext>();
                    sessions.put(newExpireTime, set);
                    set.add((BoshBackedSessionContext) value);
                    set.add(session);
                }
                ret = true;
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        for (;;) {
            if (Thread.interrupted()) {
                break;
            }

            synchronized (this) {
                try {
                    wait(CHECKING_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }

            long time = System.currentTimeMillis();

            // the inactive sessions are saved in a list to close them after the synchronized block to prevent a deadlock
            // that could happen when trying to close the sessions inside the synchronized block 
            List<BoshBackedSessionContext> inactiveSessions = null;

            synchronized (sessions) {
                // get the oldest key
                Long expireTime = sessions.isEmpty() ? null : sessions.firstKey();

                while (expireTime != null) {
                    // as long as we find expired sessions we save them in the list and remove them from our sorted map
                    if (time >= expireTime) {
                        if (inactiveSessions == null) {
                            inactiveSessions = new ArrayList<BoshBackedSessionContext>();
                        }
                        Object value = sessions.get(expireTime);
                        if (value instanceof Set) {
                            inactiveSessions.addAll((Set<BoshBackedSessionContext>) value);
                        } else if (value != null) {
                            inactiveSessions.add((BoshBackedSessionContext) value);
                        }
                        sessions.remove(expireTime);
                        expireTime = sessions.isEmpty() ? null : sessions.firstKey();
                    } else {
                        // at the first non-expired session, we know that all the next sessions are more recent and cannot
                        // be expired if the current session is Ok, so we break the loop 
                        break;
                    }
                }
            }

            if (inactiveSessions != null) {
                for (BoshBackedSessionContext session : inactiveSessions) {
                    LOGGER.error("BOSH session {} reached maximum inactivity period, closing session...", session);
                    session.close();
                }
            }
        }
    }

}
