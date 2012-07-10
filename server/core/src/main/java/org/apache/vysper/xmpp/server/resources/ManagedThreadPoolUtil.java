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
package org.apache.vysper.xmpp.server.resources;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * customizes and reports info about a thread pool
 */
public class ManagedThreadPoolUtil {

    public static void writeThreadPoolInfo(Writer writer, ThreadPoolExecutor pool) throws IOException {
        writer.append("corePool=\t").append(Integer.toString(pool.getCorePoolSize())).append("\n");
        writer.append("pool=\t\t").append(Integer.toString(pool.getPoolSize())).append("\n");
        writer.append("largestPool=\t").append(Integer.toString(pool.getLargestPoolSize())).append("\n");
        writer.append("maxPool=\t").append(Integer.toString(pool.getMaximumPoolSize())).append("\n");
        writer.append("tasks=\t\t").append(Long.toString(pool.getTaskCount())).append("\n");
        writer.append("active=\t\t").append(Integer.toString(pool.getActiveCount())).append("\n");
        writer.append("queued=\t\t").append(Integer.toString(pool.getQueue().size())).append("\n");
        writer.append("completed=\t").append(Long.toString(pool.getCompletedTaskCount())).append("\n");
    }
}
