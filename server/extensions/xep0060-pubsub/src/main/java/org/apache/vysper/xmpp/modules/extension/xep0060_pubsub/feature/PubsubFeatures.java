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
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.feature;

import org.apache.vysper.compliance.SpecCompliant;

/**
 * Static collection of known pubsub features.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
@SpecCompliant(spec = "xep-0060", section = "10", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class PubsubFeatures {
    public static final PubsubFeature ACCESS_AUTHORIZE = new PubsubFeature("access-authorize",
            "The default access model is \"authorize\".", PubsubFeatureLevel.OPTIONAL, "Nodes Access Models");

    public static final PubsubFeature ACCESS_OPEN = new PubsubFeature("access-open",
            "The default access model is \"open\".", PubsubFeatureLevel.OPTIONAL, "Nodes Access Models");

    public static final PubsubFeature ACCESS_PRESENCE = new PubsubFeature("access-presence",
            "The default access model is \"presence\".", PubsubFeatureLevel.OPTIONAL, "Nodes Access Models");

    public static final PubsubFeature ACCESS_ROSTER = new PubsubFeature("access-roster",
            "The default access model is \"roster\".", PubsubFeatureLevel.OPTIONAL, "Nodes Access Models");

    public static final PubsubFeature ACCESS_WHITELIST = new PubsubFeature("access-whitelist",
            "The default access model is \"whitelist\".", PubsubFeatureLevel.OPTIONAL, "Nodes Access Models");

    public static final PubsubFeature AUTO_CREATE = new PubsubFeature("auto-create",
            "The service supports auto-creation of nodes on publish to a non-existent node.",
            PubsubFeatureLevel.OPTIONAL, "Automatic Node Creation");

    public static final PubsubFeature AUTO_SUBSCRIBE = new PubsubFeature("auto-subscribe",
            "The service supports auto-subscription to a nodes based on presence subscription.",
            PubsubFeatureLevel.RECOMMENDED, "Auto_Subscribe");

    public static final PubsubFeature COLLECTIONS = new PubsubFeature("collections", "Collection nodes are supported.",
            PubsubFeatureLevel.OPTIONAL, "Refer to XEP-0248");

    public static final PubsubFeature CONFIG_NODE = new PubsubFeature("config-node",
            "Configuration of node options is supported.", PubsubFeatureLevel.RECOMMENDED, "Configure a Node");

    public static final PubsubFeature CREATE_AND_CONFIGURE = new PubsubFeature("create-and-configure",
            "Simultaneous creation and configuration of nodes is supported.", PubsubFeatureLevel.RECOMMENDED,
            "Create and Configure a Node");

    public static final PubsubFeature CREATE_NODES = new PubsubFeature("create-nodes",
            "Creation of nodes is supported.", PubsubFeatureLevel.RECOMMENDED, "Create a Node");

    public static final PubsubFeature DELETE_ITEMS = new PubsubFeature("delete-items",
            "Deletion of items is supported.", PubsubFeatureLevel.RECOMMENDED, "Delete an Item from a Node");

    public static final PubsubFeature DELETE_NODES = new PubsubFeature("delete-nodes",
            "Deletion of nodes is supported.", PubsubFeatureLevel.RECOMMENDED, "Delete a Node");

    public static final PubsubFeature FILTERED_NOTIFICATIONS = new PubsubFeature("filtered-notifications",
            "Notifications are filtered based on Entity Capabilities data.", PubsubFeatureLevel.RECOMMENDED,
            "Filtered Notifications");

    public static final PubsubFeature GET_PENDING = new PubsubFeature("get-pending",
            "Retrieval of pending subscription approvals is supported.", PubsubFeatureLevel.OPTIONAL,
            "Manage Subscription Requests");

    public static final PubsubFeature INSTANT_NODES = new PubsubFeature("instant-nodes",
            "Creation of instant nodes is supported.", PubsubFeatureLevel.RECOMMENDED, "Create a Node");

    public static final PubsubFeature ITEM_IDS = new PubsubFeature("item-ids",
            "Publishers may specify item identifiers.", PubsubFeatureLevel.RECOMMENDED, "");

    public static final PubsubFeature LAST_PUBLISHED = new PubsubFeature(
            "last-published",
            "By default the last published item is sent to new subscribers and on receipt of available presence from existing subscribers.",
            PubsubFeatureLevel.RECOMMENDED, "Event Types");

    public static final PubsubFeature LEASED_SUBSCRIPTION = new PubsubFeature("leased-subscription",
            "Time-based subscriptions are supported.", PubsubFeatureLevel.OPTIONAL, "Time-Based Subscriptions (Leases)");

    public static final PubsubFeature MANAGE_SUBSCRIPTIONS = new PubsubFeature("manage-subscriptions",
            "Node owners may manage subscriptions.", PubsubFeatureLevel.OPTIONAL, "Manage Subscriptions");

    public static final PubsubFeature MEMBER_AFFILIATION = new PubsubFeature("member-affiliation",
            "The member affiliation is supported.", PubsubFeatureLevel.RECOMMENDED, "Affiliations");

    public static final PubsubFeature META_DATA = new PubsubFeature("meta-data", "Node meta-data is supported.",
            PubsubFeatureLevel.RECOMMENDED, "");

    public static final PubsubFeature MODIFY_AFFILIATIONS = new PubsubFeature("modify-affiliations",
            "Node owners may modify affiliations.", PubsubFeatureLevel.OPTIONAL, "Manage Affiliations");

    public static final PubsubFeature MULTI_COLLECTION = new PubsubFeature("multi-collection",
            "A single leaf node can be associated with multiple collections.", PubsubFeatureLevel.OPTIONAL,
            "Refer to XEP-0248");

    public static final PubsubFeature MULTI_SUBSCRIBE = new PubsubFeature("multi-subscribe",
            "A single entity may subscribe to a node multiple times.", PubsubFeatureLevel.OPTIONAL,
            "Multiple Subscriptions");

    public static final PubsubFeature OUTCAST_AFFILIATION = new PubsubFeature("outcast-affiliation",
            "The outcast affiliation is supported.", PubsubFeatureLevel.RECOMMENDED, "Affiliations");

    public static final PubsubFeature PERSISTENT_ITEMS = new PubsubFeature("persistent-items",
            "Persistent items are supported.", PubsubFeatureLevel.RECOMMENDED, "");

    public static final PubsubFeature PRESENCE_NOTIFICATIONS = new PubsubFeature("presence-notifications",
            "Presence-based delivery of event notifications is supported.", PubsubFeatureLevel.OPTIONAL, "");

    public static final PubsubFeature PRESENCE_SUBSCRIBE = new PubsubFeature("presence-subscribe",
            "Authorized contacts are automatically subscribed to a user's virtual pubsub service.",
            PubsubFeatureLevel.RECOMMENDED, "Auto-Subscribe");

    public static final PubsubFeature PUBLISH = new PubsubFeature("publish", "Publishing items is supported.",
            PubsubFeatureLevel.REQUIRED, "Publish an Item to a Node");

    public static final PubsubFeature PUBLISH_OPTIONS = new PubsubFeature("publish-options",
            "Publishing an item with options is supported.", PubsubFeatureLevel.OPTIONAL, "Publishing Options");

    public static final PubsubFeature PUBLISHER_AFFILIATION = new PubsubFeature("publisher-affiliation",
            "The publisher affiliation is supported.", PubsubFeatureLevel.RECOMMENDED, "Affiliations");

    public static final PubsubFeature PURGE_NODES = new PubsubFeature("purge-nodes", "Purging of nodes is supported.",
            PubsubFeatureLevel.OPTIONAL, "Purge All Node Items");

    public static final PubsubFeature RETRACT_ITEMS = new PubsubFeature("retract-items",
            "Item retraction is supported.", PubsubFeatureLevel.OPTIONAL, "Delete an Item from a Node");

    public static final PubsubFeature RETRIEVE_AFFILIATIONS = new PubsubFeature("retrieve-affiliations",
            "Retrieval of current affiliations is supported.", PubsubFeatureLevel.RECOMMENDED, "Retrieve Affiliations");

    public static final PubsubFeature RETRIEVE_DEFAULT = new PubsubFeature("retrieve-default",
            "Retrieval of default node configuration is supported.", PubsubFeatureLevel.RECOMMENDED,
            "Request Default Configuration Options");

    public static final PubsubFeature RETRIEVE_ITEMS = new PubsubFeature("retrieve-items",
            "Item retrieval is supported.", PubsubFeatureLevel.RECOMMENDED, "Retrieve Items from a Node");

    public static final PubsubFeature RETRIEVE_SUBSCRIPTIONS = new PubsubFeature("retrieve-subscriptions",
            "Retrieval of current subscriptions is supported.", PubsubFeatureLevel.RECOMMENDED,
            "Retrieve Subscriptions");

    public static final PubsubFeature SUBSCRIBE = new PubsubFeature("subscribe",
            "Subscribing and unsubscribing are supported.", PubsubFeatureLevel.REQUIRED,
            "Subscribe to a Node and Unsubscribe from a Node");

    public static final PubsubFeature SUBSCRIPTION_OPTIONS = new PubsubFeature("subscription-options",
            "Configuration of subscription options is supported.", PubsubFeatureLevel.OPTIONAL,
            "Configure Subscription Options");

    public static final PubsubFeature SUBSCRIPTION_NOTIFICATIONS = new PubsubFeature("subscription-notifications",
            "Notification of subscription state changes is supported.", PubsubFeatureLevel.OPTIONAL,
            "Notification of Subscription State Changes");

}
