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

package org.apache.vysper.xmpp.parser;

import org.apache.xerces.xni.parser.XMLInputSource;

import java.io.StringReader;

/**
 * parses stanzas based on a CharSequence
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class StringStreamParser extends AbstractNekopullStreamParser {

    private CharSequence sourceSequence;
    private int currentSequence = 0;

    public StringStreamParser(CharSequence sourceSequence) {
        this.sourceSequence = sourceSequence;
    }

    @Override
    protected XMLInputSource getInputSource() {
        String xmlString = sourceSequence.toString();
        if (!xmlString.startsWith("<?xml")) xmlString = DEFAULT_OPENING_XML_ELEMENT + xmlString;
        return new XMLInputSource(null, null, null, new StringReader(xmlString), null);
    }

}
