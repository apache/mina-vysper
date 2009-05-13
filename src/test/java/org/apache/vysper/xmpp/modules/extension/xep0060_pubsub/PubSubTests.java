package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub;

import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.general.PubSubCreateNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.general.PubSubPublish;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.general.PubSubSubscribe;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.general.PubSubUnsubscribe;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.owner.PubSubConfigureNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.owner.PubSubDeleteNode;

import junit.framework.Test;
import junit.framework.TestSuite;

public class PubSubTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for Publish/Subscribe XEP-0060");
		//$JUnit-BEGIN$
		suite.addTestSuite(PubSubDeleteNode.class);
		suite.addTestSuite(PubSubConfigureNode.class);
		suite.addTestSuite(PubSubCreateNode.class);
		suite.addTestSuite(PubSubPublish.class);
		suite.addTestSuite(PubSubSubscribe.class);
		suite.addTestSuite(PubSubUnsubscribe.class);
		//$JUnit-END$
		return suite;
	}

}
