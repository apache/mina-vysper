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

package org.apache.vysper.xmpp.server;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;

/**
 * provides the server-side session with its context data
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface SessionContext {

    String SESSION_ATTRIBUTE_MESSAGE_STANZA_NO_RECEIVE = "stanza.message.no_receive";

    String SESSION_ATTRIBUTE_PRESENCE_STANZA_NO_RECEIVE = "stanza.presence.no_receive";

    enum SessionTerminationCause {
        /**
         * underlying connection is broken
         */
        CONNECTION_ABORT,
        /**
         * the client regularily ended the session (sending </stream:stream>)
         */
        CLIENT_BYEBYE,
        /**
         * the server is in progress of shutting down
         */
        SERVER_SHUTDOWN,
        /**
         * the server signalled a stream error to the client and subsequently needs to
         * close the session down
         */
        STREAM_ERROR;

        public static boolean isClientReceivingStanzas(SessionTerminationCause cause) {
            return cause == null || cause == SERVER_SHUTDOWN || cause == CLIENT_BYEBYE;
        }

    }

    /**
     * Gets the {@link ServerRuntimeContext}.
     *
     * @return the {@link ServerRuntimeContext}
     */
    ServerRuntimeContext getServerRuntimeContext();

    /**
     * FALSE iff _this_ server has initiated the connection (to another server), and
     * _not_ the remote side (client/server) initiated the session. for common
     * client/server connections this returns TRUE.
     */
    boolean isRemotelyInitiatedSession();

    /**
     * @return the initiating {@link Entity}. For c2s, this is the client
     *         {@link Entity}. For s2s, this is the server {@link Entity}
     */
    Entity getInitiatingEntity();

    /**
     * Sets the initiating entity. For c2s, this is the client {@link Entity}. For
     * s2s, this is the server {@link Entity}
     */
    void setInitiatingEntity(Entity entity);

    /**
     * @return <code>true</code> if this session is handling server-to-server
     *         communication (namespace "jabber:server").
     */
    boolean isServerToServer();

    /**
     * Set this session to handle server-to-server communication.
     */
    void setServerToServer();

    /**
     * Set this session to handle client-to-server communication.
     */
    void setClientToServer();

    /**
     * @return the state of this session
     */
    SessionState getState();

    /**
     * Returns the id for this session, which is unique inside a server instance and
     * across all hosted services.
     *
     * @return this session's id
     */
    String getSessionId();

    /**
     * Gets the default value for the 'xml:lang' attribute.
     *
     * @return the default language code
     */
    String getXMLLang();

    /**
     * Sets the default value for the 'xml:lang' attribute.
     *
     * @param languageCode
     *            the default language code
     */
    void setXMLLang(String languageCode);

    /**
     * Ends this session and the underlying TCP connection.
     * 
     * @param terminationCause
     *            give the logical cause for the session's end
     */
    void endSession(SessionTerminationCause terminationCause);

    /**
     * Gets the JID of the server this session is associated with.
     *
     * @return the server's JID
     */
    Entity getServerJID();

    /**
     * signals the underlying transport to handle TLS handshake
     */
    void switchToTLS(boolean delayed, boolean clientTls);

    /**
     * this method signals that from now on a new <stream:stream>... xml stream
     * begins. this is used at the very beginning of the session, then again after
     * encryption and after authentication. see RFC3920.7.5.7 and RFC3920.6.2
     */
    void setIsReopeningXMLStream();

    /**
     * binds a resource to the session
     *
     * @return resource id
     * @throws BindException
     *             when binding fails
     */
    String bindResource() throws BindException;

    /**
     * @return a value other than any previously generated values.
     */
    String nextSequenceValue();

    /**
     * put arbitrary object into the session
     * 
     * @param key
     *            identifier used to retrieve the object
     * @param value
     *            NULL to not store an object with the key
     * @return previous stored value object, or NULL
     */
    Object putAttribute(String key, Object value);

    /**
     * retrieve object
     * 
     * @param key
     *            retrieve a previously stored attribute
     * @return stored object for the given key, or NULL
     * @see org.apache.vysper.xmpp.server.SessionContext#putAttribute(String,
     *      Object)
     */
    Object getAttribute(String key);

}
