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
		START, 
		IN_TAG,
		IN_STRING,
		IN_DOUBLE_ATTRIBUTE_VALUE,
		IN_SINGLE_ATTRIBUTE_VALUE,
		IN_TEXT,
		CLOSED
	}
	
	private int lastPosition = 0;
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
     * @return the new particle or NULL, if the buffer was exhausted before the particle was completed
     * @throws Exception
     */
    public void parse(IoBuffer byteBuffer, CharsetDecoder decoder) throws SAXException {
        lastPosition = byteBuffer.position();

        while (byteBuffer.hasRemaining() && state != State.CLOSED) {
            char c = (char)byteBuffer.get();

            if(state == State.START) {
            	if(c == '<') {
            		emit(c, byteBuffer);
            		state = State.IN_TAG;
            	} else {
            		state = State.IN_TEXT;
            	}
            } else if(state == State.IN_TEXT) {
            	if(c == '<') {
            		emit(byteBuffer, decoder);
            		emit(c, byteBuffer);
            		state = State.IN_TAG;
            	}
            } else if(state == State.IN_TAG) {
            	if(c == '>') {
            		emit(c, byteBuffer);
            		state = State.START;
            	} else if(c == '"') {
                		emit(c, byteBuffer);
                		state = State.IN_DOUBLE_ATTRIBUTE_VALUE;
            	} else if(c == '\'') {
            		emit(c, byteBuffer);
            		state = State.IN_SINGLE_ATTRIBUTE_VALUE;
            	} else if(isControlChar(c)) {
            		emit(c, byteBuffer);
            	} else if(Character.isWhitespace(c)) {
            		lastPosition = byteBuffer.position();
            	} else {
            		state = State.IN_STRING;
            	}
            } else if(state == State.IN_STRING) {
            	if(c == '>') {
            		emit(byteBuffer, CharsetUtil.UTF8_DECODER);
            		emit(c, byteBuffer);
            		state = State.START;
            	} else if(isControlChar(c)) {
            		emit(byteBuffer, CharsetUtil.UTF8_DECODER);
            		emit(c, byteBuffer);
            		state = State.IN_TAG;
            	} else if(Character.isWhitespace(c)) {
            		emit(byteBuffer, CharsetUtil.UTF8_DECODER);
            		state = State.IN_TAG;
            	} else {
            		// do nothing
            	}
            } else if(state == State.IN_DOUBLE_ATTRIBUTE_VALUE) {
            	if(c == '"') {
            		emit(byteBuffer, decoder);
            		emit(c, byteBuffer);
            		state = State.IN_TAG;
            	}
            } else if(state == State.IN_SINGLE_ATTRIBUTE_VALUE) {
            	if(c == '\'') {
            		emit(byteBuffer, decoder);
            		emit(c, byteBuffer);
            		state = State.IN_TAG;
            	}
            } 
        }

        byteBuffer.position(lastPosition);
    }
    
    public void close() {
    	state = State.CLOSED;
    }
    
    private boolean isControlChar(char c) {
    	return c == '<' || c == '>' || c == '-' || c == '!' || c == '/' || c == '?' || c == '='; 
    }

    private void emit(char token, IoBuffer byteBuffer) throws SAXException {
    	listener.token(token, null);
    	
    	lastPosition = byteBuffer.position();
    }
    
    private void emit(IoBuffer byteBuffer, CharsetDecoder decoder) throws SAXException {
    	int endPosition = byteBuffer.position();
    	int oldLimit = byteBuffer.limit();
    	byteBuffer.position(lastPosition);
    	byteBuffer.limit(endPosition - 1);
		
    	try {
			listener.token(NO_CHAR, byteBuffer.getString(decoder));
		} catch (CharacterCodingException e) {
			throw new SAXException(e);
		}
		byteBuffer.limit(oldLimit);
		byteBuffer.position(endPosition);
		lastPosition = byteBuffer.position();
		
		
    }
}
