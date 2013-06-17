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
package org.apache.vysper.xml.sax.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.xml.sax.SAXException;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLTokenizer {

    private static final char NO_CHAR = (char) -1;

    private enum State {
        START, IN_TAG, IN_STRING, IN_DOUBLE_ATTRIBUTE_VALUE, IN_SINGLE_ATTRIBUTE_VALUE, IN_TEXT, CLOSED
    }

    private final IoBuffer buffer = IoBuffer.allocate(16).setAutoExpand(true);

    private State state = State.START;

    public static interface TokenListener {
        void token(char c, String token) throws SAXException;
    }

    private TokenListener listener;

    public XMLTokenizer(TokenListener listeners) {
        this.listener = listeners;
    }

    /**
     * @param byteBuffer
     * @param charsetDecoder
     * @throws Exception
     */
    public void parse(IoBuffer byteBuffer, CharsetDecoder decoder) throws SAXException {
        while (byteBuffer.hasRemaining() && state != State.CLOSED) {
            byte c = byteBuffer.get();

            if (state == State.START) {
                if (c == '<') {
                    emit(c);
                    state = State.IN_TAG;
                } else if (Character.isWhitespace(c)) {
                    // ignore
                } else {
                    state = State.IN_TEXT;
                    buffer.put(c);
                }
            } else if (state == State.IN_TEXT) {
                if (c == '<') {
                    emit(decoder);
                    emit(c);
                    state = State.IN_TAG;
                } else {
                    buffer.put(c);
                }
            } else if (state == State.IN_TAG) {
                if (c == '>') {
                    emit(c);
                    state = State.START;
                } else if (c == '"') {
                    emit(c);
                    state = State.IN_DOUBLE_ATTRIBUTE_VALUE;
                } else if (c == '\'') {
                    emit(c);
                    state = State.IN_SINGLE_ATTRIBUTE_VALUE;
                } else if (c == '-') {
                    emit(c);
                } else if (isControlChar(c)) {
                    emit(c);
                } else if (Character.isWhitespace(c)) {
                    buffer.clear();
                } else {
                    state = State.IN_STRING;
                    buffer.put(c);
                }
            } else if (state == State.IN_STRING) {
                if (c == '>') {
                    emit(CharsetUtil.getDecoder());
                    emit(c);
                    state = State.START;
                } else if (isControlChar(c)) {
                    emit(CharsetUtil.getDecoder());
                    emit(c);
                    state = State.IN_TAG;
                } else if (Character.isWhitespace(c)) {
                    emit(CharsetUtil.getDecoder());
                    state = State.IN_TAG;
                } else {
                    buffer.put(c);
                }
            } else if (state == State.IN_DOUBLE_ATTRIBUTE_VALUE) {
                if (c == '"') {
                    emit(decoder);
                    emit(c);
                    state = State.IN_TAG;
                } else {
                    buffer.put(c);
                }
            } else if (state == State.IN_SINGLE_ATTRIBUTE_VALUE) {
                if (c == '\'') {
                    emit(decoder);
                    emit(c);
                    state = State.IN_TAG;
                } else {
                    buffer.put(c);
                }
            }
        }
    }

    public void close() {
        state = State.CLOSED;
        buffer.clear();
    }

    public void restart() {
        buffer.clear();
    }

    private boolean isControlChar(byte c) {
        return c == '<' || c == '>' || c == '!' || c == '/' || c == '?' || c == '=';
    }

    private void emit(byte token) throws SAXException {
        // method will only be called for control chars, thus the cast to char should be safe
        listener.token((char)token, null);
    }

    private void emit(CharsetDecoder decoder) throws SAXException {
        try {
            buffer.flip();
            CharBuffer charBuffer = decoder.decode(buffer.buf());
            listener.token(NO_CHAR, charBuffer.toString());
            buffer.clear();
        } catch (CharacterCodingException e) {
            throw new SAXException(e);
        }
    }
}
