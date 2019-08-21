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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * NonCheckingTrustManagerFactory trust manager factory, uses an X509TrustManager implementation under the hood which 
 * will not actually do any checks.
 * 
 * nearly verbose copy from project MINA.
 * see http://svn.apache.org/viewvc/mina/branches/1.0/example/src/main/java/org/apache/mina/example/echoserver/ssl/BogusTrustManagerFactory.java?view=markup
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 */
public class NonCheckingX509TrustManagerFactory extends TrustManagerFactorySpi implements TrustManagerFactory {

    static final Logger logger = LoggerFactory.getLogger(NonCheckingX509TrustManagerFactory.class);
    
    public static final X509TrustManager X509 = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            logger.error("this XMPP Vysper instance uses NonCheckingTrustManagerFactory, clients certificates are not checked");
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            logger.error("this XMPP Vysper instance uses NonCheckingTrustManagerFactory, server certificates are not checked");
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    private static final TrustManager[] X509_MANAGERS = new TrustManager[] { X509 };

    public NonCheckingX509TrustManagerFactory() {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return X509_MANAGERS;
    }

    @Override
    protected void engineInit(KeyStore keystore) throws KeyStoreException {
        // noop
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
            throws InvalidAlgorithmParameterException {
        // noop
    }

    public TrustManager[] getTrustManagers() {
        return X509_MANAGERS;
    }
}
