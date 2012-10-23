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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 */
public class RequestsWindow {

    private final static Logger LOGGER = LoggerFactory.getLogger(RequestsWindow.class);
    
    /*
     * The highest RID that can be read and processed, this is the highest (rightmost) contiguous RID.
     * The requests from the client can come theoretically with missing updates:
     * rid_1, rid_2, rid_4 (missing rid_3, highestContinuousRid is rid_2)
     * 
     * must be synchronized along with requestsWindow 
     */
    private long highestContinuousRid = -1;

    /**
     * 
     * must be synchronized along with requestsWindow 
     */
    private long currentProcessingRequest = -1;

    /**
     * queue holding all BoshRequests, sorted ascendingly by RID.
     * 
     * NOTE: the list does not enforce the order itself, it must be sorted when adding a new request!
     * but the list always iterates ordered over Requests (which is the advantage over PriorityQueue, which 
     * needs additional object creation and CPU cycles on iteration time for ordered processing).
     * since this queue normally only holds 0, 1, 2 or only a handful of elements, and normally new additions 
     * are in-order, sorting is cheap.
     */
    protected final List<BoshRequest> queue = new ArrayList<BoshRequest>();

    protected String sessionId;
    
    protected long latestAddionTimestamp = 0;
    private Long lastKey;

    public RequestsWindow(String sessionId) {
        this.sessionId = sessionId;
    }

    
    /**
     * Returns the highest RID that is received in a continuous (uninterrupted) sequence of RIDs.
     * Higher RIDs can exist with gaps separating them from the highestContinuousRid.
     * @return the highest continuous RID received so far
     */
    public long getHighestContinuousRid() {
        return highestContinuousRid;
    }

    public long getCurrentProcessingRequest() {
        return currentProcessingRequest;
    }

    public long getLatestAddionTimestamp() {
        return latestAddionTimestamp;
    }

    public synchronized void queueRequest(BoshRequest br) {
        Long rid = br.getRid();
        if (containsRid(rid)) {
            LOGGER.warn("SID = " + sessionId + " - " + "queueing duplicated rid in requests window: " + rid);
        }
        queue.add(br);
        Collections.sort(queue);
        latestAddionTimestamp = System.currentTimeMillis();

        if (highestContinuousRid < 0 || queue.size() == 1) {
            highestContinuousRid = rid;
        }
        while (containsRid(highestContinuousRid + 1)) {
            // update the highestContinuousRid to the latest value
            // it is possible to have higher RIDs than the highestContinuousRid with a gap between them (e.g. lost client request)
            // those missing request may come late, and fill the gap, which is tracked here 
            highestContinuousRid++;
        }
        LOGGER.debug("SID = " + sessionId + " - queuing new request having rid = {}, highest continuous rid = {}", rid, highestContinuousRid);
    }
    
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
    
    public synchronized Long firstRid() {
        if (queue.isEmpty()) return null;
        final BoshRequest first = queue.get(0);
        return first.getRid();
    }

    /**
     * Returns the next BOSH body to process.
     * It is possible to have more than one BOSH body to process in the case where a lost request is resent by the client.
     * @return the next (by RID order) body to process
     */
    public synchronized BoshRequest pollNext() {
        if (queue.isEmpty()) return null;
        
        String ridSeq = logRequestWindow();

        final Long lowestRid = queue.get(0).getRid();
        currentProcessingRequest = Math.max(currentProcessingRequest, lowestRid);
        if (currentProcessingRequest > highestContinuousRid) {
            reinitHCRFromQueue();
        }
        // still a problem?
        if (currentProcessingRequest > highestContinuousRid) {
            LOGGER.debug("SID = " + sessionId + " -  using RID = NULL, not current = " + currentProcessingRequest + " " + ridSeq);
            return null; 
        }

        final BoshRequest nextRequest = queue.remove(0);
        LOGGER.debug("SID = " + sessionId + " - using RID = " + (nextRequest == null ? "NULL" : Long.toString(currentProcessingRequest)) + " " + ridSeq);
        return nextRequest;
    }

    private synchronized void reinitHCRFromQueue() {
        highestContinuousRid = -1;
        if (queue.isEmpty()) return;

        final Iterator<BoshRequest> iterator = queue.iterator();
        while (iterator.hasNext()) {
            BoshRequest boshRequest = iterator.next();            
            final Long rid = boshRequest.getRid();
            if (highestContinuousRid == -1) {
                highestContinuousRid = rid;
            } else if (highestContinuousRid + 1 == rid) {
                highestContinuousRid = rid;
            }
        }
    }

    public synchronized boolean containsRid(Long rid) {
        for (BoshRequest item : queue) {
            if (rid.equals(item.getRid())) return true;
        }
        return false;
    }

    public int size() {
        return queue.size();
    }
    
    public int getDistinctRIDs() {
        int count = 0;
        long prev = -1L;
        for (BoshRequest boshRequest : queue) {
            long rid = boshRequest.getRid();
            if (prev != rid) count++;
            prev = rid;
        }
        return count;
    }
    
    public synchronized String logRequestWindow() {
        final StringBuffer logMsg = new StringBuffer("rids = [");
        Long prev = -1L;
        for (Iterator<BoshRequest> iter = queue.iterator(); iter.hasNext(); ) {
            BoshRequest br = iter.next();
            Long currentRid = br.getRid();

            if (prev != -1 && prev != currentRid - 1) {
                logMsg.append("GAP, ");
            } 
            logMsg.append(currentRid);
            if (currentRid.equals(highestContinuousRid)) {
                logMsg.append(" HCR");
            }
            if (currentRid.equals(currentProcessingRequest)) {
                logMsg.append(" *");
            }
            if (iter.hasNext()) logMsg.append(", ");

            prev = currentRid;
        }
        logMsg.append("]");
        return logMsg.toString();
    }

    public synchronized Long getLatestRID() {
        Long rid = null;
        for (BoshRequest boshRequest : queue) {
            rid = boshRequest.getRid();
        }
        return rid;
    }
}
