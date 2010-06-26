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
package org.apache.vysper.xml.sax.perf;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class StopWatch {

    private long startTime = System.nanoTime();

    private long stopTime = -1;

    public void start() {
        startTime = System.nanoTime();
    }

    public void stop() {
        stopTime = System.nanoTime();
    }

    public String toString() {
        long endTime;
        if (stopTime == -1) {
            endTime = System.nanoTime();
        } else {
            endTime = stopTime;
        }

        double time = endTime - startTime;

        String s;
        if (time < 1000)
            s = time + " ns";
        else if (time < 1000000)
            s = (time / 1000) + " us";
        else if (time < 1000000000)
            s = (time / 1000000) + " ms";
        else
            s = (time / 1000000000) + " s";

        return s;
    }

}