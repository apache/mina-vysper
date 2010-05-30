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

$(document).ready(function() {
	// this is needed by flXHR to automatically include its dependencies
	window.flensed = {base_path:"../resources/flxhr/"};
});

$("#connect").click(function() {
	$("#connect-form").hide();
	$("#logger").show();
	connect();
});

function formatTime(val) {
	return val < 10 ? "0" + val : val;
}

function log(msg) {
	var now = new Date();
	var hours = formatTime(now.getHours());
	var minutes = formatTime(now.getMinutes());
	var seconds = formatTime(now.getSeconds());
	$("#logger").append("[" + hours + ":" + minutes + ":" + seconds + "] " + msg + "<br/>");
}

function connect() {
	server = $("#server").val();
	port = $("#port").val();
	jid = $("#jid").val();
	password = $("#password").val();
	log("Connecting to <b>" + server + ":" + port + "</b> as <b>" + jid + "</b>...");
	
	connection = new Strophe.Connection("http://" + server + ":" + port + "/");
	connection.connect(jid, password, function(status) {
		log("Connection status: " + connectionStatuses[status]);
	});
}