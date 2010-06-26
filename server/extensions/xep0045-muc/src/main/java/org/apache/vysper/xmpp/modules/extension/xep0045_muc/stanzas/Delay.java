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
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas;

import java.util.Arrays;
import java.util.Calendar;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.datetime.DateTimeProfile;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;

public class Delay extends XMLElement {

    public Delay(Entity from, Calendar timestamp) {
        super(NamespaceURIs.URN_XMPP_DELAY, "delay", null, Arrays.asList(new Attribute("from", from
                .getFullQualifiedName()), new Attribute("stamp", DateTimeProfile.getInstance().getDateTimeInUTC(
                timestamp.getTime()))), null);
    }

}
