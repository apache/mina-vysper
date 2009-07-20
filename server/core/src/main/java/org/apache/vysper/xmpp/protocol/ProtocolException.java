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
package org.apache.vysper.xmpp.protocol;

import org.apache.vysper.xmpp.stanza.Stanza;

/**
 * some error occured which is reported to the communication counterpart
 * and requires the stream to be closed
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ProtocolException extends Exception {

    private Stanza errorStanza = null;

    public ProtocolException() {
        super();
    }

    public ProtocolException(String string) {
        super(string);
    }

    public ProtocolException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public ProtocolException(Throwable throwable) {
        super(throwable);
    }

    public String getXMPPError() {
        return "bad-format";
    }

    public Stanza getErrorStanza() {
        return errorStanza;
    }

    public void setErrorStanza(Stanza errorStanza) {
        this.errorStanza = errorStanza;
    }

}
