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
package org.apache.vysper.xmpp.xmldecoder;

import org.apache.mina.common.ByteBuffer;

import java.nio.charset.CharsetDecoder;

/**
 * partitions the incoming byte stream in particles of XML. either those enclosed by '<' and '>', or the text inbetween.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ParticleDecoder {
	
	private enum State { 
		START, IN_TEXT, IN_TAG, IN_DOUBLE_ATTRIBUTE, IN_SINGLE_ATTRIBUTE, END, END_TEXT;
		
		public static boolean inAttribute(State state) {
			return state == IN_DOUBLE_ATTRIBUTE || state == IN_SINGLE_ATTRIBUTE;
		}

		public static boolean atEnd(State state) {
			return state == END || state == END_TEXT;
		}
}
	
    /**
     * split in String, either in those parts enclosed by brackets or those who are not
     * @param byteBuffer
     * @param charsetDecoder
     * @return the new particle or NULL, if the buffer was exhausted before the particle was completed
     * @throws Exception
     */
    public static XMLParticle decodeParticle(ByteBuffer byteBuffer, CharsetDecoder charsetDecoder) throws Exception {
        int startPosition = byteBuffer.position();

        State state = State.START;

        if (!byteBuffer.hasRemaining()) return null;

        // count opening and closing braces
        char firstChar = (char)byteBuffer.get();
        if(firstChar == '<') {
        	state = State.IN_TAG;
        } else {
        	state = State.IN_TEXT;
        }

        while (byteBuffer.remaining() > 0) {
            char aChar = (char)byteBuffer.get();

			if (state != State.IN_TEXT && state != State.IN_SINGLE_ATTRIBUTE && aChar == '"') {
				if (state == State.IN_DOUBLE_ATTRIBUTE) {
					state = State.IN_TAG;
				} else {
					state = State.IN_DOUBLE_ATTRIBUTE;
				}
			}
			if (state != State.IN_TEXT && state != State.IN_DOUBLE_ATTRIBUTE && aChar == '\'') {
				if (state == State.IN_SINGLE_ATTRIBUTE) {
					state = State.IN_TAG;
				} else {
					state = State.IN_SINGLE_ATTRIBUTE;
				}
			}

            if (state == State.IN_TEXT && aChar == '<') state = State.END_TEXT;
            if (state != State.IN_TEXT && !State.inAttribute(state) && aChar == '>') state = State.END;
            
            if (State.atEnd(state)) {
                int endPosition = byteBuffer.position();
                if (state == State.END_TEXT) endPosition--;
                int limit = byteBuffer.limit();
                ByteBuffer stanzaBuffer = null;
                try {
                    // prepare correct slicing
                    byteBuffer.position(startPosition);
                    byteBuffer.limit(endPosition);
                    stanzaBuffer = byteBuffer.slice();
                } finally {
                    // cut off sliced parts
                    byteBuffer.position(endPosition);
                    byteBuffer.limit(limit);
                }

                String content = stanzaBuffer.getString(charsetDecoder);
                return new XMLParticle(content);
            }
        }
        // no complete stanza found. prepare next read
        byteBuffer.position(startPosition);
        return null;
    }

}
