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
package org.apache.vysper.spring;

import org.apache.vysper.xmpp.cryptography.AbstractTLSContextFactory;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.IOException;

/**
 * helper factory class to make certificates available to MINA with Spring
 * resource injection.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ResourceBasedTLSContextFactory extends AbstractTLSContextFactory {

    private Resource certificateResource = null;

    public ResourceBasedTLSContextFactory(Resource certificateResource) {
    this.certificateResource = certificateResource;
}

    @Override
    protected InputStream getCertificateInputStream() throws IOException {
        return certificateResource.getInputStream();
    }
}
