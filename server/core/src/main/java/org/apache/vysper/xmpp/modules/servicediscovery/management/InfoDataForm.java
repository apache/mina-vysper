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
package org.apache.vysper.xmpp.modules.servicediscovery.management;

import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.FINISHED;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormEncoder;

/**
 * this adds support for Service Discovery Extensions, which allows adding x-DataForms to info responses 
 */
@SpecCompliant(spec = "XEP-0128", status = FINISHED, coverage = COMPLETE)
public class InfoDataForm implements InfoElement {

    private static final Integer CLASS_ID = 3;

    protected static final DataFormEncoder DATA_FORM_ENCODER = new DataFormEncoder();

    protected XMLElement dataFormXML;

    public InfoDataForm(DataForm dataForm) {
        dataFormXML = DATA_FORM_ENCODER.getXML(dataForm);
    }

    public Integer getElementClassId() {
        return CLASS_ID;
    }

    public void insertElement(StanzaBuilder stanzaBuilder) {
        stanzaBuilder.addPreparedElement(dataFormXML);
    }
}
