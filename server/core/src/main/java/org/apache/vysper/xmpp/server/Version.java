/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.vysper.xmpp.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * Provides the version of this release of the Vysper server
 * 
 * Code taken from FtpServer
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Version {

    private static final String FALLBACK_VERSION = "Unknown";

    /**
     * Get the version of Vysper
     * @return The current version
     */
    public static String getVersion() {
        Properties props = new Properties();
        InputStream in = null;
        
        try {
            in = Version.class.getClassLoader().getResourceAsStream("org/apache/vysper/xmpp/server/vysperserver.properties");
            if(in != null) {
                props.load(in);
                return props.getProperty("vysper.server.version");
            } else {
                return FALLBACK_VERSION;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read version", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
