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

package org.apache.vysper.xmpp.xmlfragment;


/**
 * Naive implementation, will be replaced in later stages of this change
 */
public class SimpleNamespaceResolver implements NamespaceResolver {
	
	private XMLElement elm;
	
	public SimpleNamespaceResolver(XMLElement elm) {
		this.elm = elm;
	}

	public String resolveUri(String prefix) {
		// check for the reserved xml namespace
		if(prefix.equals("xml")) {
			return NamespaceURIs.XML;
		}
		
        String xmlnsName;
        
        if(elm.getNamespacePrefix().length() > 0) {
            xmlnsName = "xmlns:" + elm.getNamespacePrefix();
        } else {
            xmlnsName = "xmlns";
        }
        
        String uri = elm.getAttributeValue(xmlnsName);
        
        // return empty string if the element is in the empty namespace
        if(uri == null) {
            uri = "";
        }
        
        return uri;
	}
	
	public String resolvePrefix(String uri) {
		// check for the reserved xml namespace
		if(uri.equals(NamespaceURIs.XML)) {
			return "xml";
		} else {
			for(Attribute attr : elm.getAttributes()) {
				if(attr instanceof NamespaceAttribute) {
					NamespaceAttribute nsAttr = (NamespaceAttribute) attr;
					if(uri.equals(nsAttr.getValue())) {
						return nsAttr.getPrefix();
					}
				}
			}
		}
		
		// URI not found
		throw new IllegalStateException("Undeclared namespace");
	}
}
