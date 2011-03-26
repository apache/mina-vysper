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
import java.util.Collection;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

/**
 * for having deploying only the BOSH context, but potentially other context
 * within the same server, for example for colocating BOSH with a chat webapp,
 * use a shared BOSH endpoint.
 */
public class SharedBoshEndpoint extends BoshEndpoint {

    protected final List<Handler> handlers = new ArrayList<Handler>();

    public void addHandler(Handler handler) {
        handlers.add(handler);
    }

    public void setHandlers(Collection<? extends Handler> moreHandlers) {
        handlers.addAll(moreHandlers);
    }
    
    @Override
    protected Handler createHandler() {

        final Handler boshHandler = super.createHandler();

        Handler[] handlerArray = new Handler[handlers.size()+1];
        handlerArray[0] = boshHandler;
        for (int i = 0; i < handlers.size(); i++) {
            handlerArray[i+1] = handlers.get(i);
        }
        
        final ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
        handlerCollection.setHandlers(handlerArray);
        return handlerCollection;
        
    }
}
