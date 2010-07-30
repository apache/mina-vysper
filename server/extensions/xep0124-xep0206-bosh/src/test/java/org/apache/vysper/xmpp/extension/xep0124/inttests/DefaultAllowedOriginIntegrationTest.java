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
package org.apache.vysper.xmpp.extension.xep0124.inttests;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.junit.Test;


/**
 * Test allowed origin when none are set for the endpoint. Should 
 * default to only allow the called domain, which is what will
 * be allowed if no crossdomain.xml or header is returned.
 *
 */
public class DefaultAllowedOriginIntegrationTest extends IntegrationTestTemplate {

    @Test
    public void flashCrossdomain() throws Exception {
        HttpResponse response = httpclient.execute(new HttpGet(getServerUrl() + "crossdomain.xml"));

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }
    
    @Test
    public void optionsAccessControlAllowOrigin() throws Exception {
        HttpResponse response = httpclient.execute(new HttpOptions(getServerUrl()));

        Assert.assertEquals(0, response.getHeaders("Access-Control-Allow-Origin").length);
    }
}
