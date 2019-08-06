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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;

/**
 * @author Réda Housni Alaoui
 */
public class SessionContextMock implements SessionContext {

    private ServerRuntimeContext serverRuntimeContext;

    private Entity initiatingEntity;

    public void givenServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void givenInitiatingEntity(Entity initiatingEntity) {
        this.initiatingEntity = initiatingEntity;
    }

    @Override
    public ServerRuntimeContext getServerRuntimeContext() {
        return serverRuntimeContext;
    }

    @Override
    public boolean isRemotelyInitiatedSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Entity getInitiatingEntity() {
        return initiatingEntity;
    }

    @Override
    public void setInitiatingEntity(Entity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isServerToServer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setServerToServer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClientToServer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionState getState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getXMLLang() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setXMLLang(String languageCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endSession(SessionTerminationCause terminationCause) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Entity getServerJID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void switchToTLS(boolean delayed, boolean clientTls) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIsReopeningXMLStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String bindResource() throws BindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String nextSequenceValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object putAttribute(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String key) {
        throw new UnsupportedOperationException();
    }
}
