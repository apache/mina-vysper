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

package org.apache.vysper.xmpp.parser.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * provide TLS communication, plus the capability to switch from plain text to
 * TLS encryption at some point
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class PlainToTLSStreaming {

    private InputStream inputStream;
    private OutputStream outputStream;

    public PlainToTLSStreaming(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public boolean switchToTLS()
    {
        return false;
    }



}
