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
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.xml.sax.SAXException;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ParticleTokenizer {
	
	private enum State { 
		START, 
		IN_TAG,
		IN_DOUBLE_ATTRIBUTE_VALUE,
		IN_SINGLE_ATTRIBUTE_VALUE,
		IN_TEXT,
		CLOSED
	}
	
	private int lastPosition = 0;
	private State state = State.START;
	
	public static interface TokenListener {
		void token(String token) throws SAXException;
	}
	
	private TokenListener listener;
	
	public ParticleTokenizer(TokenListener listeners) {
		this.listener = listeners;
	}
	
    /**
     * @param byteBuffer
     * @param charsetDecoder
     * @return the new particle or NULL, if the buffer was exhausted before the particle was completed
     * @throws Exception
     */
    public void parse(ByteBuffer byteBuffer, CharsetDecoder decoder) throws SAXException {
        lastPosition = byteBuffer.position();
//        StringBuffer sb = new StringBuffer();
        while (byteBuffer.hasRemaining() && state != State.CLOSED) {
            char c = (char)byteBuffer.get();

            if(state == State.START) {
            	if(c == '<') {
            		emit("<", byteBuffer);
            		state = State.IN_TAG;
            	} else {
            		state = State.IN_TEXT;
//            		sb.append(c);
            	}
            } else if(state == State.IN_TEXT) {
            	if(c == '<') {
            		emit(byteBuffer, decoder);
            		emit("<", byteBuffer);
            		state = State.IN_TAG;
            	} else {
//            		sb.append(c);
            	}
            } else if(state == State.IN_TAG) {
            	if(c == '/') {
            		if(checkEmit(byteBuffer)) {
            			emit(byteBuffer, decoder);
            		}
            		emit("/", byteBuffer);
            	} else if(c == '>') {
            		if(checkEmit(byteBuffer)) {
            			emit(byteBuffer, decoder);
            		}
                	emit(">", byteBuffer);
                	state = State.START;
            	} else if(Character.isWhitespace(c)) {
            		if(checkEmit(byteBuffer)) {
            			emit(byteBuffer, decoder);
            		} else {
            			// ignore whitespace
            			lastPosition = byteBuffer.position();
            		}
            	} else if(c == '=') {
            		emit(byteBuffer, decoder);
            		emit("=", byteBuffer);
            	} else if(c == '"') {
            		lastPosition = byteBuffer.position();
//            		emit("\"", byteBuffer);
            		state = State.IN_DOUBLE_ATTRIBUTE_VALUE;
            	} else if(c == '\'') {
//            		emit("\'", byteBuffer);
            		lastPosition = byteBuffer.position();
            		state = State.IN_SINGLE_ATTRIBUTE_VALUE;
            	} else {
            		// non-whitespace char
            	}
            } else if(state == State.IN_DOUBLE_ATTRIBUTE_VALUE) {
            	if(c == '"') {
            		emit(byteBuffer, decoder);
//            		emit("\"", byteBuffer);
            		state = State.IN_TAG;
            	}
            } else if(state == State.IN_SINGLE_ATTRIBUTE_VALUE) {
            	if(c == '\'') {
            		emit(byteBuffer, decoder);
//            		emit("'", byteBuffer);
            		state = State.IN_TAG;
            	}
            } 
        }

        byteBuffer.position(lastPosition);
    }
    
    public void close() {
    	state = State.CLOSED;
    }
    
    private boolean checkEmit(ByteBuffer buffer) {
    	return buffer.position() > lastPosition + 1;
    }
    
    private void emit(String token, ByteBuffer byteBuffer) throws SAXException {
    	listener.token(token);
    	
    	lastPosition = byteBuffer.position();
    }

    private void emit(ByteBuffer byteBuffer, CharsetDecoder decoder) throws SAXException {
    	int endPosition = byteBuffer.position();
    	int oldLimit = byteBuffer.limit();
    	byteBuffer.position(lastPosition);
    	byteBuffer.limit(endPosition - 1);
		
    	try {
			listener.token(byteBuffer.getString(decoder));
		} catch (CharacterCodingException e) {
			throw new SAXException(e);
		}
		byteBuffer.limit(oldLimit);
		byteBuffer.position(endPosition);
		lastPosition = byteBuffer.position();
		
		
    }
}
