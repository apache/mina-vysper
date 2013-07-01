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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * derived from MINA's BogusSSLContextFactory.
 * see http://svn.apache.org/viewvc/mina/branches/1.0/example/src/main/java/org/apache/mina/example/echoserver/ssl/BogusSSLContextFactory.java?view=markup
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractTLSContextFactory implements TLSContextFactory {

    private static final String PROTOCOL = "TLS";

    private static final String DEFAULT_ALGORITHM = "SunX509";

    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";

    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) algorithm = DEFAULT_ALGORITHM;
        
        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    private SSLContext sslContext = null;

    protected String password = null;
    
    private String keystoreType = DEFAULT_KEYSTORE_TYPE;

    protected TrustManagerFactory trustManagerFactory = null;

    // NOTE: The keystore 'bogus_mina_tls.cert' was generated using keytool:
    //   keytool -genkey -alias bogus -keysize 512 -validity 3650
    //           -keyalg RSA -dname "CN=bogus.com, OU=XXX CA,
    //               O=BogusTrustManagerFactory Inc, L=Stockholm, S=Stockholm, C=SE"
    //           -keypass boguspw -storepass boguspw -keystore bogus.cert

    abstract protected InputStream getCertificateInputStream() throws IOException;

    public void setPassword(String password) {
        this.password = password;
    }

    public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
        this.trustManagerFactory = trustManagerFactory;
    }

    public void setKeyStoreType(String keyStoreType) {
    	this.keystoreType = keyStoreType;
    }
    
    public SSLContext getSSLContext() throws GeneralSecurityException, IOException {
        if (sslContext == null)
            sslContext = createSSLContext();
        return sslContext;
    }

    private SSLContext createSSLContext() throws GeneralSecurityException, IOException {
        // Create keystore
        KeyStore ks = KeyStore.getInstance(keystoreType);
        InputStream in = null;
        try {
            in = getCertificateInputStream();
            ks.load(in, password.toCharArray());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    ;
                }
            }
        }

        // Set up key manager factory to use our key store
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        kmf.init(ks, password.toCharArray());

        // Initialize the SSLContext to work with our key managers.
        SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
        
        TrustManager[] trustManagers = null; // this is the default
        if  (trustManagerFactory != null) {
            // override the default with configured ones 
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        sslContext.init(kmf.getKeyManagers(), trustManagers, null);

        return sslContext;
    }

}
