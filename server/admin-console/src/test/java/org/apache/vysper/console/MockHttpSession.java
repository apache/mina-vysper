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
package org.apache.vysper.console;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

@SuppressWarnings("deprecation")
public class MockHttpSession implements HttpSession {
    private Map<String, Object> attributes = new HashMap<String, Object>();

    public void setMaxInactiveInterval(int interval) {
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void removeValue(String name) {
        removeAttribute(name);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    public boolean isNew() {
        return false;
    }

    public void invalidate() {
        
    }

    public String[] getValueNames() {
        return null;
    }

    public Object getValue(String name) {
        return getAttribute(name);
    }

    public HttpSessionContext getSessionContext() {
        return null;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public int getMaxInactiveInterval() {
        return 0;
    }

    public long getLastAccessedTime() {
        return 0;
    }

    public String getId() {
        return null;
    }

    public long getCreationTime() {
        return 0;
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getAttributeNames() {
        return null;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }
}