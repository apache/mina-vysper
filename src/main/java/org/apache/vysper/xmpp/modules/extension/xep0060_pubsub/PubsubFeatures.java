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

/**
 * Static collection of known pubsub features.
 * 
 * @author The Apache MINA Project (http://mina.apache.org)
 */
public class PubsubFeatures {
    public static final PubsubFeature access_authorize = new PubsubFeature("access-authorize", "The default access model is \"authorize\".", "OPTIONAL", "Nodes Access Models");
    public static final PubsubFeature access_open = new PubsubFeature("access-open", "The default access model is \"open\".", "OPTIONAL", "Nodes Access Models");
    public static final PubsubFeature access_presence = new PubsubFeature("access-presence", "The default access model is \"presence\".", "OPTIONAL", "Nodes Access Models");
    public static final PubsubFeature access_roster = new PubsubFeature("access-roster", "The default access model is \"roster\".", "OPTIONAL", "Nodes Access Models");
    public static final PubsubFeature access_whitelist = new PubsubFeature("access-whitelist", "The default access model is \"whitelist\".", "OPTIONAL", "Nodes Access Models");
    public static final PubsubFeature auto_create = new PubsubFeature("auto-create", "The service supports auto-creation of nodes on publish to a non-existent node.", "OPTIONAL", "Automatic Node Creation");
    public static final PubsubFeature auto_subscribe = new PubsubFeature("auto-subscribe", "The service supports auto-subscription to a nodes based on presence subscription.", "RECOMMENDED", "Auto_Subscribe");
    public static final PubsubFeature collections = new PubsubFeature("collections", "Collection nodes are supported.", "OPTIONAL", "Refer to XEP-0248");
    public static final PubsubFeature config_node = new PubsubFeature("config-node", "Configuration of node options is supported.", "RECOMMENDED", "Configure a Node");
    public static final PubsubFeature create_and_configure = new PubsubFeature("create-and-configure", "Simultaneous creation and configuration of nodes is supported.", "RECOMMENDED", "Create and Configure a Node");
    public static final PubsubFeature create_nodes = new PubsubFeature("create-nodes", "Creation of nodes is supported.", "RECOMMENDED", "Create a Node");
    public static final PubsubFeature delete_items = new PubsubFeature("delete-items", "Deletion of items is supported.", "RECOMMENDED", "Delete an Item from a Node");
    public static final PubsubFeature delete_nodes = new PubsubFeature("delete-nodes", "Deletion of nodes is supported.", "RECOMMENDED", "Delete a Node");
    public static final PubsubFeature filtered_notifications = new PubsubFeature("filtered-notifications", "Notifications are filtered based on Entity Capabilities data.", "RECOMMENDED", "Filtered Notifications");
    public static final PubsubFeature get_pending = new PubsubFeature("get-pending", "Retrieval of pending subscription approvals is supported.", "OPTIONAL", "Manage Subscription Requests");
    public static final PubsubFeature instant_nodes = new PubsubFeature("instant-nodes", "Creation of instant nodes is supported.", "RECOMMENDED", "Create a Node");
    public static final PubsubFeature item_ids = new PubsubFeature("item-ids", "Publishers may specify item identifiers.", "RECOMMENDED", "");
    public static final PubsubFeature last_published = new PubsubFeature("last-published", "By default the last published item is sent to new subscribers and on receipt of available presence from existing subscribers.", "RECOMMENDED", "Event Types");
    public static final PubsubFeature leased_subscription = new PubsubFeature("leased-subscription", "Time-based subscriptions are supported.", "OPTIONAL", "Time-Based Subscriptions (Leases)");
    public static final PubsubFeature manage_subscriptions = new PubsubFeature("manage-subscriptions", "Node owners may manage subscriptions.", "OPTIONAL", "Manage Subscriptions");
    public static final PubsubFeature member_affiliation = new PubsubFeature("member-affiliation", "The member affiliation is supported.", "RECOMMENDED", "Affiliations");
    public static final PubsubFeature meta_data = new PubsubFeature("meta-data", "Node meta-data is supported.", "RECOMMENDED", "");
    public static final PubsubFeature modify_affiliations = new PubsubFeature("modify-affiliations", "Node owners may modify affiliations.", "OPTIONAL", "Manage Affiliations");
    public static final PubsubFeature multi_collection = new PubsubFeature("multi-collection", "A single leaf node can be associated with multiple collections.", "OPTIONAL", "Refer to XEP-0248");
    public static final PubsubFeature multi_subscribe = new PubsubFeature("multi-subscribe", "A single entity may subscribe to a node multiple times.", "OPTIONAL", "Multiple Subscriptions");
    public static final PubsubFeature outcast_affiliation = new PubsubFeature("outcast-affiliation", "The outcast affiliation is supported.", "RECOMMENDED", "Affiliations");
    public static final PubsubFeature persistent_items = new PubsubFeature("persistent-items", "Persistent items are supported.", "RECOMMENDED", "");
    public static final PubsubFeature presence_notifications = new PubsubFeature("presence-notifications", "Presence-based delivery of event notifications is supported.", "OPTIONAL", "");
    public static final PubsubFeature presence_subscribe = new PubsubFeature("presence-subscribe", "Authorized contacts are automatically subscribed to a user's virtual pubsub service.", "RECOMMENDED", "Auto-Subscribe");
    public static final PubsubFeature publish = new PubsubFeature("publish", "Publishing items is supported.", "REQUIRED", "Publish an Item to a Node");
    public static final PubsubFeature publish_options = new PubsubFeature("publish-options", "Publishing an item with options is supported.", "OPTIONAL", "Publishing Options");
    public static final PubsubFeature publisher_affiliation = new PubsubFeature("publisher-affiliation", "The publisher affiliation is supported.", "RECOMMENDED", "Affiliations");
    public static final PubsubFeature purge_nodes = new PubsubFeature("purge-nodes", "Purging of nodes is supported.", "OPTIONAL", "Purge All Node Items");
    public static final PubsubFeature retract_items = new PubsubFeature("retract-items", "Item retraction is supported.", "OPTIONAL", "Delete an Item from a Node");
    public static final PubsubFeature retrieve_affiliations = new PubsubFeature("retrieve-affiliations", "Retrieval of current affiliations is supported.", "RECOMMENDED", "Retrieve Affiliations");
    public static final PubsubFeature retrieve_default = new PubsubFeature("retrieve-default", "Retrieval of default node configuration is supported.", "RECOMMENDED", "Request Default Configuration Options");
    public static final PubsubFeature retrieve_items = new PubsubFeature("retrieve-items", "Item retrieval is supported.", "RECOMMENDED", "Retrieve Items from a Node");
    public static final PubsubFeature retrieve_subscriptions = new PubsubFeature("retrieve-subscriptions", "Retrieval of current subscriptions is supported.", "RECOMMENDED", "Retrieve Subscriptions");
    public static final PubsubFeature subscribe = new PubsubFeature("subscribe", "Subscribing and unsubscribing are supported.", "REQUIRED", "Subscribe to a Node and Unsubscribe from a Node");
    public static final PubsubFeature subscription_options = new PubsubFeature("subscription-options", "Configuration of subscription options is supported.", "OPTIONAL", "Configure Subscription Options");
    public static final PubsubFeature subscription_notifications = new PubsubFeature("subscription-notifications", "Notification of subscription state changes is supported.", "OPTIONAL", "Notification of Subscription State Changes");
}
