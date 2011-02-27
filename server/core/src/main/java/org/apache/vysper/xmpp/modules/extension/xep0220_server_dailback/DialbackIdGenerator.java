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
package org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.Validate;
import org.apache.vysper.xmpp.addressing.Entity;


public class DialbackIdGenerator {

    // generates a shared secret within the server
    private static final String SECRET = UUID.randomUUID().toString();
    private SecretKeySpec signingKey = new SecretKeySpec(DigestUtils.sha256(SECRET), "HmacSHA256");
    private Mac mac;
    
    public DialbackIdGenerator() {
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
    
    public String generate(Entity receiving, Entity originating, String streamId) {
        Validate.notNull(receiving, "receiving can not be null");
        Validate.notNull(originating, "originating can not be null");
        Validate.notNull(streamId, "streamId can not be null");
        
        try {
            mac.update(receiving.getDomain().getBytes("UTF-16"));
            mac.update(" ".getBytes("UTF-16"));
            mac.update(originating.getDomain().getBytes("UTF-16"));
            mac.update(" ".getBytes("UTF-16"));
            mac.update(streamId.getBytes("UTF-16"));
    
            byte[] rawHmac = mac.doFinal();
            return Hex.encodeHexString(rawHmac);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean verify(String dailbackId, Entity receiving, Entity originating, String streamId) {
        return dailbackId.equals(generate(receiving, originating, streamId));
    }
}
