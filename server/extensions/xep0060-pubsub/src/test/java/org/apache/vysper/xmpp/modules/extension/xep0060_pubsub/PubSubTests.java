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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.disco.PubSubDiscoInfoTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.disco.PubSubDiscoItemsTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubCreateNodeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubPublishTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubRetrieveSubscriptionsTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubSubscribeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.PubSubUnsubscribeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubConfigureNodeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubDeleteNodeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubOwnerModifyAffiliationsTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler.owner.PubSubOwnerRetrieveAffiliationsTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNodeTestCase;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNodeTestCase;

/**
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class PubSubTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for Publish/Subscribe XEP-0060");
        //$JUnit-BEGIN$
        suite.addTestSuite(PubSubDeleteNodeTestCase.class);
        suite.addTestSuite(PubSubConfigureNodeTestCase.class);
        suite.addTestSuite(PubSubOwnerRetrieveAffiliationsTestCase.class);
        suite.addTestSuite(PubSubOwnerModifyAffiliationsTestCase.class);

        suite.addTestSuite(PubSubCreateNodeTestCase.class);
        suite.addTestSuite(PubSubPublishTestCase.class);
        suite.addTestSuite(PubSubSubscribeTestCase.class);
        suite.addTestSuite(PubSubUnsubscribeTestCase.class);
        suite.addTestSuite(PubSubRetrieveSubscriptionsTestCase.class);

        suite.addTestSuite(CollectionNodeTestCase.class);
        suite.addTestSuite(LeafNodeTestCase.class);

        suite.addTestSuite(PubSubDiscoInfoTestCase.class);
        suite.addTestSuite(PubSubDiscoItemsTestCase.class);
        //$JUnit-END$
        return suite;
    }

}
