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
package org.apache.vysper.xmpp.extension.xep0065_socks;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} for a SOCKS5 proxy in accordance with RFC 1928 and XEP-0065.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class Socks5AcceptorHandler extends IoHandlerAdapter {

    public static final AttributeKey STATE_KEY = new AttributeKey(Socks5AcceptorHandler.class, "state");

    public static final AttributeKey PAIR_KEY = new AttributeKey(Socks5AcceptorHandler.class, "pair");

    private static final byte SOCKS_VERSION = 5;

    private static final byte AUTH_NO_AUTH = 0;

    private static final byte AUTH_USERNAME_PASSWORD = 2;

    private static final byte AUTH_NOT_SUPPORTED = 0x55;

    public enum ProxyState {
        OPENED, INITIATED, CONNECTED, CLOSED
    }

    private final Logger log = LoggerFactory.getLogger(Socks5AcceptorHandler.class);

    private Socks5ConnectionsRegistry connections;

    public enum Socks5AuthType {
        NO_AUTH, USERNAME_PASSWORD
    }

    /**
     * Packet parsing for SOCKS5 opening packets
     */
    public static class OpeningPacket {

        private EnumSet<Socks5AuthType> authTypes;

        public OpeningPacket(IoBuffer buffer) {
            if (buffer.remaining() < 3)
                throw new IllegalArgumentException();

            byte socksVersion = buffer.get();
            if (socksVersion != SOCKS_VERSION)
                throw new IllegalArgumentException();

            byte noOfAuth = buffer.get();
            if (buffer.remaining() != noOfAuth)
                throw new IllegalArgumentException();

            byte[] authMethods = new byte[noOfAuth];
            buffer.get(authMethods);

            List<Socks5AuthType> authTypes = new ArrayList<Socks5AuthType>();
            for (byte a : authMethods) {
                if (a == AUTH_NO_AUTH)
                    authTypes.add(Socks5AuthType.NO_AUTH);
                else if (a == AUTH_USERNAME_PASSWORD)
                    authTypes.add(Socks5AuthType.USERNAME_PASSWORD);
            }
            this.authTypes = EnumSet.copyOf(authTypes);
        }

        public EnumSet<Socks5AuthType> getAuthTypes() {
            return authTypes;
        }

        public boolean isAuthSupported() {
            return authTypes.contains(Socks5AuthType.NO_AUTH);
        }

        public IoBuffer createResponse() {
            if (isAuthSupported()) {
                // no auth
                return IoBuffer.wrap(new byte[] { SOCKS_VERSION, AUTH_NO_AUTH });
            } else {
                // not supported
                return IoBuffer.wrap(new byte[] { SOCKS_VERSION, AUTH_NOT_SUPPORTED });
            }
        }
    }

    /**
     * Packet parsing for SOCKS5 initating packets
     */
    public static class InitiatingPacket {

        private static final byte RESERVED = 0;
        private static final byte REQUEST_GRANTED = 0;
        private static final byte ADDR_TYPE_NAME = 0x03;

        // TODO charset?
        private static final String CHARSET = "ASCII";
        
        private byte addrType;
        private byte[] addressBuffer;
        private String address;
        private short port;

        public InitiatingPacket(IoBuffer buffer) {
            if (buffer.remaining() < 9)
                throw new IllegalArgumentException();

            byte socksVersion = buffer.get();
            if (socksVersion != SOCKS_VERSION) {
                throw new IllegalArgumentException();
            }
            
            byte cmdCode = buffer.get();
            if (cmdCode != 1) {
                throw new IllegalArgumentException("Only supports TCP stream connections");
            }
            
            byte reserved = buffer.get();
            if (reserved != 0) {
                throw new IllegalArgumentException("Reserved bit must be 0");
            }
            
            addrType = buffer.get();
            if (addrType == ADDR_TYPE_NAME) {
                addressBuffer = new byte[buffer.get()];
            } else {
                throw new IllegalArgumentException("Must use domain name address type");
            }
            
            buffer.get(addressBuffer);

            try {
                this.address = new String(addressBuffer, CHARSET);
            } catch (UnsupportedEncodingException ignored) {
                ;
            }
            this.port = buffer.getShort();
        }

        public String getAddress() {
            return address;
        }

        public short getPort() {
            return port;
        }

        public IoBuffer createResponse() {
            IoBuffer b = IoBuffer.allocate(10).setAutoExpand(true);
            b.put(SOCKS_VERSION);
            b.put(REQUEST_GRANTED); // approved
            b.put(RESERVED); // reserved
            b.put(addrType);
            b.put((byte) addressBuffer.length);
            b.put(addressBuffer);
            b.putShort(port);

            return b.flip();
        }
    }

    public Socks5AcceptorHandler(Socks5ConnectionsRegistry connections) {
        this.connections = connections;
    }

    /**
     * {@inheritDoc}
     */
    public void sessionOpened(IoSession session) throws Exception {
        log.info("SOCKS5 session opened");
        session.setAttribute(STATE_KEY, ProxyState.OPENED);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        log.info("SOCKS5 connection idle, will be closed");
        Socks5Pair pair = getPair(session);
        if (pair != null) {
            pair.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void messageReceived(final IoSession session, Object message) throws Exception {
        IoBuffer buffer = (IoBuffer) message;

        ProxyState state = getState(session);

        try {
            if (state == ProxyState.OPENED) {
                OpeningPacket packet = new OpeningPacket(buffer);

                if (packet.isAuthSupported()) {
                    // no auth
                    setState(session, ProxyState.INITIATED);
                    session.write(packet.createResponse());
                    log.info("SOCKS5 session initiated");
                } else {
                    // not supported
                    session.write(packet.createResponse());
                    session.close(false);
                    log.info("SOCKS5 session closed");
                }

            } else if (state == ProxyState.INITIATED) {
                InitiatingPacket packet = new InitiatingPacket(buffer);

                session.write(packet.createResponse());
                setState(session, ProxyState.CONNECTED);

                String hash = packet.getAddress();
                Socks5Pair pair = connections.register(hash, session);

                session.setAttribute(PAIR_KEY, pair);
            } else {
                Socks5Pair pair = getPair(session);
                if (pair != null && pair.isActivated()) {
                    pair.getOther(session).write(message);
                } else {
                    // writing before activated, close
                    pair.close();
                }
            }
        } catch (IllegalArgumentException e) {
            session.close(false);
            log.info("SOCKS5 session closed");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sessionClosed(IoSession session) throws Exception {
        log.info("SOCKS5 connection closed");
        Socks5Pair pair = getPair(session);
        if (pair != null) {
            pair.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        log.error("Exception caused in SOCKS5 proxy, probably a bug in Vysper", cause);
        Socks5Pair pair = getPair(session);
        
        if(pair != null) {
            pair.close();
        } else {
            session.close(false);
        }
    }

    private void setState(final IoSession session, ProxyState state) {
        session.setAttribute(STATE_KEY, state);
    }

    private ProxyState getState(final IoSession session) {
        return (ProxyState) session.getAttribute(STATE_KEY);
    }

    private Socks5Pair getPair(final IoSession session) {
        Socks5Pair pair = (Socks5Pair) session.getAttribute(PAIR_KEY);
        return pair;
    }
}