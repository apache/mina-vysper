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
package org.apache.vysper.xmpp.modules.core.base.handler.async;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.vysper.xmpp.modules.core.base.handler.IQHandler;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;

/**
 * info/query (IQ) stanzas might better be process asynchronously, for example if a remote system is creating the
 * response. this handler provides a framework for doing so.
 *
 * Two components must be provided by implementations of this class: an Executor and a Task factory method.
 * Implementations of createGetTask() create the backend specific task (a Runnable). The returned future provides
 * access to the result.
 * The executor provides (access to) the async execution enviroment for the task. 
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
abstract public class AbstractAsyncIQGetHandler extends IQHandler {

    /**
     * handles the actutal business logic invocation (separate thread, remote call etc.)
     */
    protected Executor serviceExecutor;

    abstract protected RunnableFuture<XMPPCoreStanza> createGetTask(IQStanza stanza,
            ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext);

    @Override
    protected List<Stanza> executeIQLogic(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, boolean outboundStanza,
                                          SessionContext sessionContext, StanzaBroker stanzaBroker) {
        switch (stanza.getIQType()) {
        case GET:
            executeGetIQLogicAsync(stanza, serverRuntimeContext, sessionContext);
            return Collections.emptyList(); // IQ response is sent later

            // all non-GET requests are handled synchronously, regardless of  
        case ERROR:
        case RESULT:
        case SET: // TODO make 'set'-IQs async'able, too.
        default:
            return executeNonGetIQLogic(stanza, serverRuntimeContext, sessionContext);
        }
    }

    protected void executeGetIQLogicAsync(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
        // create the object which handles building and sending out the response as
        // soon as the serices makes the result available
        RunnableFuture<XMPPCoreStanza> task = createGetTask(stanza, serverRuntimeContext, sessionContext);
        // must return immediately by running invokation in another process
        serviceExecutor.execute(task);
    }

    /**
     * override this method, if you want to handle other types than get.
     * @return error stanza
     */
    protected List<Stanza> executeNonGetIQLogic(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
        throw new RuntimeException("iq stanza type not supported: " + stanza.getIQType().value());
    }
}
