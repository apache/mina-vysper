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
package org.apache.vysper.charset;

import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * utility class for charsets
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class CharsetUtil {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static ThreadLocal<CharsetDecoder> decoderCache = new ThreadLocal<CharsetDecoder>();
    private static ThreadLocal<CharsetEncoder> encoderCache = new ThreadLocal<CharsetEncoder>();

    private static Object getReference(ThreadLocal threadLocal) {
        SoftReference reference = (SoftReference) threadLocal.get();
        if (reference == null) return null; 
        return reference.get();
    }

    private static void setReference(ThreadLocal threadLocal, Object object) {
        threadLocal.set(new SoftReference(object));
    }

    public static CharsetEncoder getEncoder() {
        CharsetEncoder encoder = (CharsetEncoder) getReference(encoderCache);
        if (encoder == null) {
            encoder = UTF8.newEncoder();
            setReference(encoderCache, encoder);
        }
        return encoder;
    }

    public static CharsetDecoder getDecoder() {
        CharsetDecoder decoder = (CharsetDecoder) getReference(decoderCache);
        if (decoder == null) {
            decoder = UTF8.newDecoder();
            setReference(decoderCache, decoder);
        }
        return decoder;
    }
}