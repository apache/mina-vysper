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
package org.apache.vysper.storage.jcr;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.vysper.xmpp.addressing.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * back-end stuff for JCR, used by the semantic specific adapters
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class JcrStorage {

    final Logger logger = LoggerFactory.getLogger(JcrStorage.class);

    protected static JcrStorage jcrStorageSingleton;

    protected JcrStorage() {
        super();
        // protected
    }

    public static JcrStorage getInstance() {
        synchronized (JcrStorage.class) {
            if (jcrStorageSingleton == null)
                jcrStorageSingleton = new JcrStorage();
            return jcrStorageSingleton;
        }
    }

    protected Session session = null;

    public Session getRepositorySession() throws JcrStorageException {
        if (session != null)
            return session;
        try {
            Repository repository = new TransientRepository();
            session = repository.login(new SimpleCredentials("xmpp-admin", "adminpassword".toCharArray()));
            return session;
        } catch (Exception e) {
            throw new JcrStorageException(e);
        }
    }

    public Node getRootNode() throws JcrStorageException {
        try {
            return getRepositorySession().getRootNode();
        } catch (RepositoryException e) {
            throw new JcrStorageException(e);
        }
    }

    public Node getEntityNode(Entity bareEntity, String namespace, boolean createIfMissing) throws JcrStorageException {
        bareEntity = bareEntity.getBareJID(); // make it really sure
        if (namespace != null)
            namespace = namespace.replace(':', '_');
        final String path = "/accountentity/" + bareEntity.getFullQualifiedName()
                + (namespace != null ? "/" + namespace : "");
        if (!itemExists(path)) {
            if (!createIfMissing)
                return null;
            Node accountEntityNode = getOrCreate(getRootNode(), "accountentity");
            Node entityNode = getOrCreate(accountEntityNode, bareEntity.getFullQualifiedName());
            if (namespace != null)
                entityNode = getOrCreate(entityNode, namespace);
            return entityNode;
        } else {
            try {
                return (Node) getRepositorySession().getItem(path);
            } catch (RepositoryException e) {
                throw new JcrStorageException(e);
            }
        }
    }

    private boolean itemExists(String absolutePath) throws JcrStorageException {
        try {
            return getRepositorySession().itemExists(absolutePath);
        } catch (RepositoryException e) {
            throw new JcrStorageException(e);
        } catch (JcrStorageException e) {
            throw e;
        }
    }

    protected Node getOrCreate(Node parent, String nodeName) throws JcrStorageException {
        Node childNode;
        try {
            childNode = parent.getNode(nodeName);
        } catch (RepositoryException e) {
            childNode = null;
        }
        if (childNode == null) {
            try {
                childNode = parent.addNode(nodeName);
                parent.save();
                childNode.save();
                logger.info("JCR node created: " + childNode);
            } catch (RepositoryException e) {
                throw new JcrStorageException(e);
            }
        }
        return childNode;
    }
}
