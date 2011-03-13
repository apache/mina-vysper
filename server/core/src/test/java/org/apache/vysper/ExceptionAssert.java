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

package org.apache.vysper;

import org.junit.Assert;

public class ExceptionAssert {

    private static final String MESSAGE = "msg";
    private static final Throwable CAUSE = new RuntimeException();
    
    public static void assertConstructors(Class<? extends Exception> exceptionClass) throws Exception {
        
        // assert default cstr 
        Exception e = exceptionClass.newInstance();
        Assert.assertNull(e.getMessage());
        Assert.assertNull(e.getCause());

        // assert String cstr 
        e = exceptionClass.getConstructor(String.class).newInstance(MESSAGE);
        Assert.assertEquals(MESSAGE, e.getMessage());
        Assert.assertNull(e.getCause());
        
        // assert throwable cstr 
        e = exceptionClass.getConstructor(Throwable.class).newInstance(CAUSE);
        Assert.assertEquals(CAUSE.getClass().getName(), e.getMessage());
        Assert.assertEquals(CAUSE, e.getCause());
        
        // assert String, Throwable cstr 
        e = exceptionClass.getConstructor(String.class, Throwable.class).newInstance(MESSAGE, CAUSE);
        Assert.assertEquals(MESSAGE, e.getMessage());
        Assert.assertEquals(CAUSE, e.getCause());
    }
}
