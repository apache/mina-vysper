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
	$("#disconnect").click(disconnect);
	$("#chat").click(chat);
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
	// get roster
	var iq = $iq({type: 'get'}).c('query', {xmlns: 'jabber:iq:roster'});
	log("Requesting roster", iq.toString());
	connection.sendIQ(iq, rosterReceived);
	
	// handle received messages
	connection.addHandler(messageReceived, null, "message", "chat");
	
	$("#chat, #disconnect").removeAttr("disabled");
}

function rosterReceived(iq) {
	log("Received roster", Strophe.serialize(iq));
	$(iq).find("item").each(function() {
		// if a contact is still pending subscription then do not show it in the list
		if ($(this).attr('ask')) {
			return true;
		}
		var jid = $(this).attr('jid');
		$("#roster").append("<option value='" + jid + "'>" + jid + "</option>");
	});
}

function jid2id(jid) {
	return jid.replace("@", "AT").replace(/\./g, "_").replace(/\//g, "-");
}

function messageReceived(msg) {
	var jid = $(message).attr("from");
	var id = jid2id(jid);
	if ($("#" + id).length === 0) {
		initChatArea(jid, id);
	}
	$("#tabs").tabs("select", "#" + id);
	$("#" + id + " > input").focus();
}

function initChatArea(jid, id) {
	$("#tabs").tabs("add", id, jid);
	$("#" + id).append("<div></div><input type='text'/>");
	$("#" + id).attr("jid", jid);
}

function disconnect() {
	$("#roster").empty();
	$("#chat, #disconnect").attr("disabled", "disabled");
	log("Disconnecting...");
	connection.disconnect();
}

function chat() {
	
}