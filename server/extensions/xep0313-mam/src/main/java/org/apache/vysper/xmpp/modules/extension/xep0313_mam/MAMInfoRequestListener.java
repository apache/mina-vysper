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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam;

import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.xmpp.modules.servicediscovery.management.Feature;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequestListener;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

/**
 * @author Réda Housni Alaoui
 */
public class MAMInfoRequestListener implements InfoRequestListener {
    @Override
    public List<InfoElement> getInfosFor(InfoRequest request) {
        List<InfoElement> infoElements = new ArrayList<>();
        infoElements.add(new Feature(NamespaceURIs.XEP0359_STANZA_IDS));
        infoElements.add(new Feature(NamespaceURIs.JABBER_X_DATA));
        return infoElements;
    }
}
