/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.vysper.mina.codec;

import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.TransportType;

import java.net.SocketAddress;
import java.util.Set;

public class MockIoSession extends BaseIoSession
{

    @Override
    protected void updateTrafficMask()
    {
    }

    public IoSessionConfig getConfig()
    {
        return null;
    }

    public IoFilterChain getFilterChain()
    {
        return null;
    }

    public IoHandler getHandler()
    {
        return null;
    }

    public SocketAddress getLocalAddress()
    {
        return null;
    }

    public SocketAddress getRemoteAddress()
    {
        return null;
    }

    public int getScheduledWriteBytes()
    {
        return 0;
    }

    public int getScheduledWriteRequests()
    {
        return 0;
    }

    public IoService getService()
    {
        return null;
    }

    public SocketAddress getServiceAddress()
    {
        return null;
    }

    public IoServiceConfig getServiceConfig()
    {
        return null;
    }

    public TransportType getTransportType()
    {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return super.getAttribute(s);
    }

    @Override
    public Object setAttribute(String s, Object o) {
        return super.setAttribute(s, o);
    }

    @Override
    public Object setAttribute(String s) {
        return super.setAttribute(s);
    }

    @Override
    public Object removeAttribute(String s) {
        return super.removeAttribute(s);
    }

    @Override
    public boolean containsAttribute(String s) {
        return super.containsAttribute(s);
    }

    @Override
    public Set<String> getAttributeKeys() {
        return super.getAttributeKeys();
    }
}
