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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.extension.xep0124.BoshEndpoint;
import org.apache.vysper.xmpp.extension.xep0124.XMLUtil;
import org.junit.Test;



public class AllowedOriginIntegrationTest extends IntegrationTestTemplate {

    @Override
    protected BoshEndpoint processBoshEndpoint(BoshEndpoint endpoint) {
        List<String> allowedDomains = new ArrayList<String>();
        allowedDomains.add("example.com");
        allowedDomains.add("foo.example.com");
        endpoint.setAccessControlAllowOrigin(allowedDomains);
        
        return endpoint;
    }
    
    private String parseFully(InputStream stream) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        while(line != null) {
            sb.append(line);
            line = reader.readLine();
        }
        return sb.toString();
    }
    
    @Test
    public void flashCrossdomain() throws Exception {
        HttpResponse response = httpclient.execute(new HttpGet(getServerUrl() + "crossdomain.xml"));

        String declaration = "<?xml version='1.0'?><!DOCTYPE cross-domain-policy SYSTEM 'http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd'>";
        
        // XML parser does not support DOCTYPE declarations, so we hack around that here
        String xml = parseFully(response.getEntity().getContent());
        Assert.assertTrue(xml.startsWith(declaration));
        
        xml = xml.substring(declaration.length());
        
        XMLElement elm = new XMLUtil(xml).parse();
        Assert.assertEquals("cross-domain-policy", elm.getName());
        
        Assert.assertEquals(2, elm.getInnerElements().size());
        
        XMLElement firstChild = elm.getInnerElements().get(0);
        XMLElement secondChild = elm.getInnerElements().get(1);
        
        Assert.assertEquals("allow-access-from", firstChild.getName());
        Assert.assertEquals("example.com", firstChild.getAttributeValue("domain"));
        
        Assert.assertEquals("allow-access-from", secondChild.getName());
        Assert.assertEquals("foo.example.com", secondChild.getAttributeValue("domain"));
    }
    
    @Test
    public void optionsAccessControlAllowOrigin() throws Exception {
        HttpResponse response = httpclient.execute(new HttpOptions(getServerUrl()));

        Assert.assertEquals(1, response.getHeaders("Access-Control-Allow-Origin").length);
        String accessControlAllowOriginHeader = response.getHeaders("Access-Control-Allow-Origin")[0].getValue();

        Assert.assertEquals("example.com,foo.example.com", accessControlAllowOriginHeader);
    }
}
