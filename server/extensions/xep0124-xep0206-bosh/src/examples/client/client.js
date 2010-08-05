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

var server;
var port;
var jid;
var password;
var connection;
var subscribeRequests = [];
var isDisconnecting = false;

var connectionStatuses = {};
connectionStatuses[Strophe.Status.ERROR] = "ERROR";
connectionStatuses[Strophe.Status.CONNECTING] = "CONNECTING";
connectionStatuses[Strophe.Status.CONNFAIL] = "CONNFAIL";
connectionStatuses[Strophe.Status.AUTHENTICATING] = "AUTHENTICATING";
connectionStatuses[Strophe.Status.AUTHFAIL] = "AUTHFAIL";
connectionStatuses[Strophe.Status.CONNECTED] = "CONNECTED";
connectionStatuses[Strophe.Status.DISCONNECTED] = "DISCONNECTED";
connectionStatuses[Strophe.Status.DISCONNECTING] = "DISCONNECTING";
connectionStatuses[Strophe.Status.ATTACHED] = "ATTACHED";

// this is needed by flXHR to automatically include its dependencies
window.flensed.base_path="../resources/flxhr/";

$(document).ready(function() {
	$("#tabs").tabs();
	$("#roster").dialog({
		autoOpen: false,
		buttons: {"Disconnect": disconnect, "Add contact...": addContact},
		closeOnEscape: false,
		width: 400,
		height: 250,
		position: ["right", "top"],
		title: "Roster",
		beforeclose: function() {return isDisconnecting;}
	});

	$("#connect").click(function() {
		$("#connect-form").hide();
		$("#workspace").show();
		connect();
	});
});

function getSubscribeRequest(jid) {
    for (var i = 0; i < subscribeRequests.length; i++) {
        if (jid === subscribeRequests[i]) {
            return i;
        }
    }
    return -1;
}

function formatTime(val) {
	return val < 10 ? "0" + val : val;
}

function log(msg, xml) {
	var now = new Date();
	var hours = formatTime(now.getHours());
	var minutes = formatTime(now.getMinutes());
	var seconds = formatTime(now.getSeconds());
	var m = "[" + hours + ":" + minutes + ":" + seconds + "] " + msg;
	if (xml) {
		xml = xml.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&apos;");
		m += ": " + xml;
	}
	m += "<br/>";
	$("#logger").append(m);
}

Strophe.log = function (level, msg) {
	if(console && console.log) console.log(msg)
};

function connect() {
	server = $("#server").val();
	port = $("#port").val();
	jid = $("#jid").val();
	password = $("#password").val();
	log("Connecting to <b>" + server + ":" + port + "</b> as <b>" + jid + "</b>...");
	
	connection = new Strophe.Connection("http://" + server + ":" + port + "/");

	connection.connect(jid, password, function(status) {
		log("Connection status: " + connectionStatuses[status]);
		if (status === Strophe.Status.CONNECTED) {
			userConnected();
		} else if (status === Strophe.Status.DISCONNECTED) {
			$("#workspace").hide();
			$("#connect-form").show();
			$("#logger").empty();
		}
	});
}

function userConnected() {
	getRoster();
	// handle received messages
	connection.addHandler(messageReceived, null, "message", "chat");
	
	// handle presence
	connection.addHandler(presenceReceived, null, "presence");	
	isDisconnecting = false;
	$("#roster").dialog("open");	
}

function getRoster() {
	var iq = $iq({type: 'get'}).c('query', {xmlns: 'jabber:iq:roster'});
	log("Requesting roster", iq.toString());
	connection.sendIQ(iq, rosterReceived);
}

function rosterReceived(iq) {
	log("Received roster", Strophe.serialize(iq));
	$("#roster").empty();

	$(iq).find("item").each(function() {
		// if a contact is still pending subscription then do not show it in the list
		if ($(this).attr('ask')) {
			return true;
		}
		var jid = $(this).attr('jid');
		$("#roster").append("<div jid='" + jid2id(jid) + "'>" + jid + " (offline)</div>");
	});
	log("Sending my presence", $pres().toString());
	connection.send($pres());
}

function jid2id(jid) {
	return Strophe.getBareJidFromJid(jid).replace("@", "AT").replace(/\./g, "_");
}

function messageReceived(msg) {
	log("Received chat message", Strophe.serialize(msg));
	var jid = $(msg).attr("from");
	var bareJid = Strophe.getBareJidFromJid(jid);
	var id = jid2id(jid);

	$("#tabs").show()
	
	if ($("#chat" + id).length === 0) {
		$("#tabs").tabs("add", "#chat" + id, bareJid);
		$("#chat" + id).append("<div style='height: 290px; margin-bottom: 10px; overflow: auto;'></div><input type='text' style='width: 100%;'/>");
	}
	
	if($(msg).find("> body")) {
		$("#chat" + id + " > div").append("<p>" + $(msg).find("> body").text() + "</p>");
		$("#chat" + id + " > div").get(0).scrollTop = $("#chat" + id + " > div").get(0).scrollHeight;
	}
	
	$("#chat" + id).data("jid", jid);
	$("#tabs").tabs("select", "#chat" + id);
	$("#chat" + id + " > input").focus();

	return true;
}


function disconnect() {
	isDisconnecting = true;
	$("#roster").dialog("close");
	$("#roster").empty();
	
 	log("Disconnecting...");
	
	connection.send($pres({type: "unavailable"}));
	connection.flush();
	connection.disconnect();
}

function chat() {
	
}

function presenceReceived(presence) {
	log("Received presence", Strophe.serialize(presence));
	var fromJid = $(presence).attr('from');
	var bareFromJid = Strophe.getBareJidFromJid(fromJid);
	var type = $(presence).attr('type');
	var id = jid2id(fromJid);
	if (type === "error") {
		alert("Received presence error!");
	} else if (type === "subscribe") {
		if (confirm(fromJid + " wants to subscribe to your presence. Do you allow it?")) {
			var pres = $pres({to: fromJid, type: "subscribed"});
			log("Allowing subscribe", pres.toString());
			connection.send(pres);
			if ($("#roster > div[jid=" + id + "]").length === 0) {
				$("#roster").append("<div jid='" + id + "'>" + bareFromJid + " (offline)</div>");
				pres = $pres({to: fromJid, type: "subscribe"});
				log("Requesting subscribe from " + fromJid, pres.toString());
				connection.send(pres);
			}
		} else {
			var pres = $pres({to: fromJid, type: "unsubscribed"});
			log("Denying subscribe", pres.toString());
			connection.send(pres);
		}
	} else if (bareFromJid !== jid) {
		$("#roster > div[jid=" + id + "]").text(bareFromJid + " (" + (type === "unavailable" ? "offline" : "online") + ")");
		if (getSubscribeRequest(bareFromJid) === -1) {
			log("Sending presence", $pres().toString());
			connection.send($pres());
		} else {
			subscribeRequests.splice(getSubscribeRequest(bareFromJid), 1);
		}
	}
	return true;
}

function addContact() {
	var toJid = prompt("Please type the JID of the contact you want to add");
	var id = jid2id(toJid);
	if (toJid === null) {
		return;
	}
	if (toJid === jid) {
		alert("You cannot add yourself to the roster!");
		return;
	}
	if ($("#roster > div[jid=" + id + "]").length > 0) {
		alert("JID already present in the roster!");
		return;
	}
	$("#roster").append("<div jid='" + jid2id(toJid) + "'>" + toJid + " (offline)</div>");
	subscribeRequests.push(toJid);
	var pres = $pres({to: toJid, type: "subscribe"});
	log("Requesting subscribe to " + toJid, pres.toString());
	connection.send(pres);
}