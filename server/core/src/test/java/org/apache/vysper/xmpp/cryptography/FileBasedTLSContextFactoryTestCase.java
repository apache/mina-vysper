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
package org.apache.vysper.xmpp.cryptography;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import junit.framework.Assert;

import org.junit.Test;

/**
 */
public class FileBasedTLSContextFactoryTestCase extends TLSContextFactoryTestTemplate {

    protected AbstractTLSContextFactory createTLSContextFactory() {
        return createTLSContextFactory("src/main/config/bogus_mina_tls.cert");
    }
    
    protected AbstractTLSContextFactory createTLSContextFactory(String path) {
        File keystore = new File(path);
        return new FileBasedTLSContextFactory(keystore);
    }
    
    @Test(expected=IOException.class)
    public void nonExistingKeystoreFile() throws GeneralSecurityException, IOException {
        AbstractTLSContextFactory contextFactory = createTLSContextFactory("dummy");
        
        contextFactory.setPassword("boguspw");
        
        Assert.assertNotNull(contextFactory.getSSLContext());
    }
}
