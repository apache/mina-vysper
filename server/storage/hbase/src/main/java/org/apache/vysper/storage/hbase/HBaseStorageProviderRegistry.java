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
package org.apache.vysper.storage.hbase;

import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.storage.hbase.privatedata.HBasePrivateDataPersistenceManager;
import org.apache.vysper.storage.hbase.roster.HBaseRosterManager;
import org.apache.vysper.storage.hbase.user.HBaseUserManagement;
import org.apache.vysper.storage.hbase.vcard.HBaseVcardTempPersistenceManager;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class HBaseStorageProviderRegistry extends OpenStorageProviderRegistry {

    public HBaseStorageProviderRegistry() throws HBaseStorageException {
        add(new HBaseUserManagement(HBaseStorage.getInstance()));
        add(new HBaseRosterManager(HBaseStorage.getInstance()));
        add(new HBasePrivateDataPersistenceManager(HBaseStorage.getInstance()));
        add(new HBaseVcardTempPersistenceManager(HBaseStorage.getInstance()));
    }

}
