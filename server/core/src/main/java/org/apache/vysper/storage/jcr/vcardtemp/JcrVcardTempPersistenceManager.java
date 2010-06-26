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
package org.apache.vysper.storage.jcr.vcardtemp;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.vysper.storage.jcr.JcrStorage;
import org.apache.vysper.storage.jcr.JcrStorageException;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempPersistenceManager;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class JcrVcardTempPersistenceManager implements VcardTempPersistenceManager {

    final Logger logger = LoggerFactory.getLogger(JcrVcardTempPersistenceManager.class);

    protected JcrStorage jcrStorage;

    public JcrVcardTempPersistenceManager(JcrStorage jcrStorage) {
        this.jcrStorage = jcrStorage;
    }

    public boolean isAvailable() {
        Session session = null;
        try {
            session = jcrStorage.getRepositorySession();
            return session != null;
        } catch (JcrStorageException e) {
            return false;
        }
    }

    public String getVcard(Entity entity) {
        Node entityNode = getEntityNodeSave(entity, false);
        if (entityNode == null)
            return null;
        try {
            return entityNode.getProperty("content").getString();
        } catch (RepositoryException e) {
            return null;
        }
    }

    private Node getEntityNodeSave(Entity entity, boolean createIfMissing) {
        Node entityNode;
        try {
            entityNode = jcrStorage.getEntityNode(entity.getBareJID(), NamespaceURIs.VCARD_TEMP, createIfMissing);
        } catch (JcrStorageException e) {
            return null;
        }
        if (entityNode == null)
            return null;
        return entityNode;
    }

    public boolean setVcard(Entity entity, String xml) {
        Node entityNode = getEntityNodeSave(entity, true);
        try {
            entityNode.setProperty("content", xml);
            entityNode.save();
            logger.info("JCR node created: " + entityNode);
            return true;
        } catch (RepositoryException e) {
            return false;
        }
    }
}
