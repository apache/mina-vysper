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
if(window.flensed) window.flensed.base_path="../resources/flxhr/";

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
	$("#logger").get(0).scrollTop = $("#logger").get(0).scrollHeight;
}

Strophe.log = function (level, msg) {
	if(typeof console != "undefined" && console.log) console.log(msg)
	
};

function connect() {
	server = $("#server").val();
	port = $("#port").val();
	contextPath = $("#contextPath").val();
	jid = $("#jid").val();
	password = $("#password").val();
	log("Connecting to <b>" + server + ":" + port + "/" + contextPath + "</b> as <b>" + jid + "</b>...");
	
	connection = new Strophe.Connection("http://" + server + ":" + port + "/" + contextPath);

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
		addToRoster($(this).attr('jid'));
	});
	log("Sending my presence", $pres().toString());
	connection.send($pres());
}

function addToRoster(jid) {
	var id = jid2id(jid);
	$("#roster").append("<div style='cursor: pointer;' jid='" + id + "'>" + jid + " (offline)</div>");
	$("#roster > div[jid=" + id + "]").click(function() {
		chatWith(jid);
	});
	$("#roster > div[jid=" + id + "]").hover(function() {
		$(this).css("color", "red");
	}, function() {
		$(this).css("color", "#333333");
	});
}

function jid2id(jid) {
	return Strophe.getBareJidFromJid(jid).replace("@", "AT").replace(/\./g, "_");
}

function messageReceived(msg) {
	log("Received chat message", Strophe.serialize(msg));
	var jid = $(msg).attr("from");
	
	verifyChatTab(jid);
	
	var body = $(msg).find("> body");
	if (body.length === 1) {
		showMessage(jid2id(jid), jid, body.text());
	}
	return true;
}

function showMessage(tabId, authorJid, text) {
	var bareJid = Strophe.getBareJidFromJid(authorJid);
	var chat = $("#chat" + tabId + " > div");
	if (chat.length === 0) {
		return;
	}
	chat.append("<div><b>" + bareJid + "</b>: " + text + "</div>");
	chat.get(0).scrollTop = chat.get(0).scrollHeight;
	$("#tabs").tabs("select", "#chat" + tabId);
	$("#chat" + tabId + " > input").focus();
}

function verifyChatTab(jid) {
	var id = jid2id(jid);
	var bareJid = Strophe.getBareJidFromJid(jid);
	$("#tabs").show();
	if ($("#chat" + id).length === 0) {
		$("#tabs").tabs("add", "#chat" + id, bareJid);
		$("#chat" + id).append("<div style='height: 290px; margin-bottom: 10px; overflow: auto;'></div><input type='text' style='width: 100%;'/>");
		$("#chat" + id).data("jid", jid);
		$("#chat" + id + " > input").keydown(function(event) {
			if (event.which === 13) {
				event.preventDefault();
				sendMessage($(this).parent().data("jid"), $(this).val());
				$(this).val("");
			}
		});
	}
	$("#tabs").tabs("select", "#chat" + id);
	$("#chat" + id + " > input").focus();
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

function chatWith(toJid) {
	log("Chatting with " + toJid + "...");
	verifyChatTab(toJid);
}

function sendMessage(toJid, text) {
	showMessage(jid2id(toJid), jid, text);
    var msg = $msg({to: toJid, "type": "chat"}).c('body').t(text);
    log("Sending message", Strophe.serialize(msg));
    connection.send(msg);
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
				addToRoster(fromJid);
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
		var contact = $("#roster > div[jid=" + id + "]");
		if (contact.length === 1) {
			var isOnline = contact.text().match(/.+\(online\)/);
			if (isOnline && type === "unavailable") {
				contact.text(bareFromJid + " (offline)");
			} else if (!isOnline && type !== "unavailable") {
				contact.text(bareFromJid + " (online)");
				log("Sending presence", $pres().toString());
				connection.send($pres());
			}
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
	addToRoster(toJid);
	var pres = $pres({to: toJid, type: "subscribe"});
	log("Requesting subscribe to " + toJid, pres.toString());
	connection.send(pres);
}