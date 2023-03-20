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

/**
 * {@code <bad-format/>} -- the entity has sent XML that cannot be processed;
 * this error MAY be used instead of the more specific XML-related
 * errors, such as {@code <bad-namespace-prefix/>}, {@code <invalid-xml/>},
 * {@code <restricted-xml/>}, {@code <unsupported-encoding/>}, and
 * {@code <xml-not-well-formed/>}, although the more specific errors are
 * preferred.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ParsingException extends Exception {

    private ParsingErrorCondition errorCondition = ParsingErrorCondition.BAD_FORMAT;

    public ParsingException() {
        super(); //To change body of overridden methods use File | Settings | File Templates.
    }

    public ParsingException(String string) {
        super(string); //To change body of overridden methods use File | Settings | File Templates.
    }

    public ParsingException(String string, Throwable throwable) {
        super(string, throwable); //To change body of overridden methods use File | Settings | File Templates.
    }

    public ParsingException(Throwable throwable) {
        super(throwable); //To change body of overridden methods use File | Settings | File Templates.
    }

    public ParsingErrorCondition getErrorCondition() {
        return errorCondition;
    }
}
