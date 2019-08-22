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
package org.apache.vysper.storage;

/**
 * This registry bundles storage providers. It is primarily intended for
 * grouping for one kind of underlying storage, for example in-memory, SQL
 * or JCR, but custom mixed collections are possible. 
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface StorageProviderRegistry {

    /**
     * access the ready-to-go storage provider instance implementing the
     * given interface
     * @param clazz a class implementing StorageProvider 
     * @return the fully initialized storage provider
     */
    <T extends StorageProvider> T retrieve(Class<T> clazz);

    /**
     * adds a storage provider implementation to the registry
     * @param storageProvider
     */
    void add(StorageProvider storageProvider);

    /**
     * adds a storage provider implementaton to the registry
     * the registry is responsible for instantiating the given class 
     * @param storageProviderFullQualifiedClassname fully qualified
     * class name of the implementation. the implementation must have a public
     * no-argument constructor
     */
    void add(String storageProviderFullQualifiedClassname);
}
