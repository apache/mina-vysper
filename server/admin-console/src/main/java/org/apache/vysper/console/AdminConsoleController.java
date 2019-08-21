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
package org.apache.vysper.console;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.commands.AdHocCommand.Action;
import org.jivesoftware.smackx.commands.AdHocCommand.Status;
import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * MVC controller
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@Controller
public class AdminConsoleController {

    private static final String MODEL_AUTHENTICATED = "authenticated";

    public static final String SESSION_ATTRIBUTE = "smack.client";

    public static final String SESSION_FIELD = "vysper-admingui-sessionid";

    public static final Map<String, String> COMMANDS = new HashMap<String, String>();
    static {
        COMMANDS.put("get-online-users-num", "Get online users");
        COMMANDS.put("add-user", "Add user");
        COMMANDS.put("change-user-password", "Change user password");
    }

    private XMPPTCPConnectionConfiguration connectionConfiguration;

    private AdHocCommandDataBuilder adHocCommandDataBuilder = new AdHocCommandDataBuilder();

    private HtmlFormBuilder htmlFormBuilder = new HtmlFormBuilder();

    @Autowired
    public AdminConsoleController(XMPPTCPConnectionConfiguration connectionConfiguration) {
        this.connectionConfiguration = connectionConfiguration;
    }

    /**
     * Show the first page
     */
    @RequestMapping("/")
    public ModelAndView index(HttpSession session) throws XMPPException {
        ExtendedXMPPConnection client = (ExtendedXMPPConnection) session.getAttribute(SESSION_ATTRIBUTE);
        if (client == null) {
            // login
            return login();
        } else if (!client.isConnected()) {
            return login("Disconnected from XMPP server, please log in again");
        } else {
            ModelAndView mav = new ModelAndView("index");
            mav.addObject(MODEL_AUTHENTICATED, getUserName(client));
            return mav;
        }
    }

    private String getUserName(XMPPConnection client) {
        Entity entity = EntityImpl.parseUnchecked(client.getUser().toString());
        return entity.getBareJID().getFullQualifiedName();
    }

    /**
     * Show the initial command form
     */
    @RequestMapping(value = "/{command}", method = RequestMethod.GET)
    public ModelAndView command(@PathVariable("command") String command, HttpSession session) throws XMPPException {
        ExtendedXMPPConnection client = (ExtendedXMPPConnection) session.getAttribute(SESSION_ATTRIBUTE);
        if (client == null) {
            // login
            return login();
        } else if (!client.isConnected()) {
            return login("Disconnected from XMPP server, please log in again");
        } else {
            if (!COMMANDS.keySet().contains(command)) {
                throw new ResourceNotFoundException();
            }

            AdHocCommandData requestCommand = new AdHocCommandData();
            requestCommand.setType(Type.set);
            requestCommand.setFrom(client.getUser());
            requestCommand.setTo(client.getXMPPServiceDomain());
            requestCommand.setAction(Action.execute);
            requestCommand.setNode("http://jabber.org/protocol/admin#" + command);

            return sendRequestAndGenerateForm(command, client, requestCommand);
        }
    }

    /**
     * Handle a submitted form and show the result or additional form
     */
    @RequestMapping(value = "/{command}", method = RequestMethod.POST)
    public ModelAndView submitCommand(@PathVariable("command") String command, HttpServletRequest request,
            HttpSession session) throws XMPPException {
        ExtendedXMPPConnection client = (ExtendedXMPPConnection) session.getAttribute(SESSION_ATTRIBUTE);
        if (client == null) {
            // login
            return login();
        } else if (!client.isConnected()) {
            return login("Disconnected from XMPP server, please log in again");
        } else {
            @SuppressWarnings("unchecked")
            AdHocCommandData requestCommand = adHocCommandDataBuilder.build(request.getParameterMap());
            requestCommand.setType(Type.set);
            requestCommand.setFrom(client.getUser());
            requestCommand.setTo(client.getXMPPServiceDomain());
            requestCommand.setNode("http://jabber.org/protocol/admin#" + command);

            return sendRequestAndGenerateForm(command, client, requestCommand);
        }
    }

    private ModelAndView sendRequestAndGenerateForm(String command, ExtendedXMPPConnection client,
            AdHocCommandData requestCommand) {
        try {
            Stanza response = client.sendSync(requestCommand);

            StringBuffer htmlForm = new StringBuffer();
            if (response != null) {
                AdHocCommandData responseData = (AdHocCommandData) response;
                DataForm form = responseData.getForm();

                for (AdHocCommandNote note : responseData.getNotes()) {
                    htmlForm.append("<p class='note " + note.getType() + "'>" + note.getValue() + "</p>");
                }

                htmlForm.append("<form action='' method='post'>");
                htmlForm.append("<input type='hidden' name='" + SESSION_FIELD + "' value='"
                        + responseData.getSessionID() + "' />");

                htmlForm.append(htmlFormBuilder.build(form));
                if (Status.executing.equals(responseData.getStatus())) {
                    htmlForm.append("<input type='submit' value='" + COMMANDS.get(command) + "' />");
                } else if (Status.completed.equals(responseData.getStatus())) {
                    if (form == null || form.getFields() == null || form.getFields().isEmpty()) {
                        // no field, print success
                        htmlForm.append("<p>Command successful</p>");
                    }
                }
                htmlForm.append("</form>");
            } else {
                htmlForm.append("<p class='note error'>Timeout waiting for response from XMPP server</p>");

            }

            ModelAndView mav = new ModelAndView("command");
            mav.addObject(MODEL_AUTHENTICATED, getUserName(client));
            mav.addObject("form", htmlForm.toString());
            return mav;
        } catch (InterruptedException | SmackException.NotConnectedException e) {
            throw new RuntimeException(e);
        }
    }

    private ModelAndView login() {
        return login("Please log in");
    }

    private ModelAndView login(String msg) {
        ModelAndView mav = new ModelAndView("index");
        mav.addObject("message", msg);
        return mav;
    }

    protected ExtendedXMPPConnection createXMPPConnection() {
        return new ExtendedXMPPConnection(connectionConfiguration);
    }

    /**
     * Connect and authenticate the user
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ModelAndView login(@RequestParam("username") String username, @RequestParam("password") String password,
            HttpSession session) {
        ExtendedXMPPConnection client = createXMPPConnection();
        try {
            client.connect();
            client.login(username, password);
            session.setAttribute(SESSION_ATTRIBUTE, client);
            return new ModelAndView("redirect:");
        } catch (XMPPException | SmackException | IOException | InterruptedException e) {
            ModelAndView mav = new ModelAndView("index");
            mav.addObject("error", "Failed to login to server: " + e.getMessage());
            return mav;
        }
    }

    /**
     * Log out and disconnect the user
     */
    @RequestMapping(value = "/logout")
    public ModelAndView logout(HttpSession session) {
        ExtendedXMPPConnection client = (ExtendedXMPPConnection) session.getAttribute(SESSION_ATTRIBUTE);
        if (client != null) {
            client.disconnect();
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
        return new ModelAndView("redirect:");
    }

}
