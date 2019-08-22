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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * multi-purpose storage provider registry. it is recommended to re-use this
 * class as a base class for own extensions.
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class OpenStorageProviderRegistry implements StorageProviderRegistry {

    protected final Logger logger = LoggerFactory.getLogger(OpenStorageProviderRegistry.class);

    protected enum OverridePolicy {
        ERROR, WARN, ACCEPT
    };

    protected Map<Class<? extends StorageProvider>, StorageProvider> storageServices = new HashMap<Class<? extends StorageProvider>, StorageProvider>();

    protected OverridePolicy overridePolicy = OverridePolicy.ERROR;

    public void setEntries(Map<Class<? extends StorageProvider>, StorageProvider> entries) {
        storageServices.putAll(entries);
    }

    @SuppressWarnings("unchecked")
    public <T extends StorageProvider> T retrieve(Class<T> clazz) {
        return (T) storageServices.get(clazz);
    }

    /**
     * for this instance of storage provider, adds all parent interfaces to the registry,
     * whose direct parent interface is StorageProvider
     * 
     * @param storageProvider
     */
    public void add(StorageProvider storageProvider) {
        if (storageProvider == null)
            throw new IllegalArgumentException("storage service must not be NULL!");
        Class<? extends StorageProvider> clazz = storageProvider.getClass();

        Set<Class> storageProviderInterfaces = getStorageProviderInterfaces(clazz);

        for (Class storageProviderInterface : storageProviderInterfaces) {
            addInternal(storageProviderInterface, storageProvider);
        }
    }

    public void add(String storageProviderFQClassname) {
        Class<StorageProvider> storageProviderClass;
        try {
            storageProviderClass = (Class<StorageProvider>) Class.forName(storageProviderFQClassname);
        } catch (ClassCastException e) {
            logger.info("not a Vysper storage provider class: " + storageProviderFQClassname);
            return;
        } catch (ClassNotFoundException e) {
            logger.info("could not load storage provider class " + storageProviderFQClassname);
            return;
        }
        try {
            StorageProvider storageProvider = storageProviderClass.newInstance();
            add(storageProvider);
        } catch (Exception e) {
            logger.info("failed to instantiate storage provider class " + storageProviderFQClassname);
            return;
        }
    }

    private Set<Class> getStorageProviderInterfaces(Class clazz) {
        Set<Class> storageProviderInterfaces = new HashSet<Class>();

        Class[] classes = clazz.getInterfaces(); // all directly implemented interfaces
        for (Class aClass : classes) {
            Class[] parentInterfaces = aClass.getInterfaces();
            for (Class parentInterface : parentInterfaces) {
                if (parentInterface == StorageProvider.class) {
                    storageProviderInterfaces.add(aClass);
                    break;
                }
            }
        }

        if (storageProviderInterfaces.size() == 0 && clazz.getSuperclass() != null) {
            // we don't have one, maybe superclass has. go up the hierarchie
            storageProviderInterfaces = getStorageProviderInterfaces(clazz.getSuperclass());
        }

        return storageProviderInterfaces;
    }

    private void addInternal(Class<? extends StorageProvider> interfaceClass, StorageProvider storageProvider) {
        // handle overriding
        switch (overridePolicy) {
        case ACCEPT:
            break; // fall through to put 
        case ERROR:
            if (retrieve(interfaceClass) != null)
                throw new IllegalStateException("storage service already registered for class "
                        + interfaceClass.getCanonicalName());
        case WARN:
            if (retrieve(interfaceClass) != null)
                logger.warn("storage service already registered for class " + interfaceClass.getCanonicalName());
            break; // fall through to put
        default:
            throw new IllegalStateException("unknown override policy state: " + overridePolicy.name());
        }

        storageServices.put(interfaceClass, storageProvider);
    }
}
