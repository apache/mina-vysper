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
package org.apache.vysper.xml.fragment;

/**
 * the XML was syntactically ok, but business logic applied on xml level revealed semantical problems
 * (wrong attributes, doubled elements etc.)
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XMLSemanticError extends Exception {
    public XMLSemanticError() {
        super();
    }

    public XMLSemanticError(String s) {
        super(s);
    }

    public XMLSemanticError(String s, Throwable throwable) {
        super(s, throwable);
    }

    public XMLSemanticError(Throwable throwable) {
        super(throwable);
    }
}
