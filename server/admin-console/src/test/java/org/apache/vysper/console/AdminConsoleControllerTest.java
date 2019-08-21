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

import javax.servlet.http.HttpServletRequest;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

import static org.mockito.Mockito.mock;


public class AdminConsoleControllerTest {
    
    private static final String ENTITY = "test@vysper.org";
    private static final String PASSWORD = "password";

    private ExtendedXMPPConnection connection = mock(ExtendedXMPPConnection.class);
    
    private AdminConsoleController controller = new AdminConsoleController(null) {
        @Override
        protected ExtendedXMPPConnection createXMPPConnection() {
            return connection;
        }
    };
    
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private MockHttpSession session = new MockHttpSession();
    
    @Test
    public void login() throws XMPPException, InterruptedException, IOException, SmackException {
        ModelAndView mav = controller.login(ENTITY, PASSWORD, session);
        
        Mockito.verify(connection).connect();
        Mockito.verify(connection).login(ENTITY, PASSWORD);
        
        Assert.assertNotNull(session.getAttribute(AdminConsoleController.SESSION_ATTRIBUTE));
        Assert.assertEquals("redirect:", mav.getViewName());
    }

    @Test
    public void failedLogin() throws XMPPException, InterruptedException, IOException, SmackException {
        XMPPException xmppException = mock(XMPPException.class);
        Mockito.doThrow(xmppException).when(connection).login(ENTITY, PASSWORD);
        
        ModelAndView mav = controller.login(ENTITY, PASSWORD, session);
        
        Mockito.verify(connection).connect();
        Mockito.verify(connection).login(ENTITY, PASSWORD);
        
        Assert.assertNull(session.getAttribute(AdminConsoleController.SESSION_ATTRIBUTE));
        Assert.assertEquals("index", mav.getViewName());
        Assert.assertNotNull(mav.getModel().get("error"));
        Assert.assertNull(mav.getModel().get("authenticated"));
    }

    @Test
    public void failedConnect() throws XMPPException, InterruptedException, IOException, SmackException {
        XMPPException xmppException = mock(XMPPException.class);
        Mockito.doThrow(xmppException).when(connection).connect();
        
        ModelAndView mav = controller.login(ENTITY, PASSWORD, session);
        
        Mockito.verify(connection).connect();
        
        Assert.assertNull(session.getAttribute(AdminConsoleController.SESSION_ATTRIBUTE));
        Assert.assertEquals("index", mav.getViewName());
        Assert.assertNotNull(mav.getModel().get("error"));
        Assert.assertNull(mav.getModel().get("authenticated"));
    }

    @Test
    public void indexNotAuthenticated() throws XMPPException {
        ModelAndView mav = controller.index(session);
        
        Assert.assertEquals("index", mav.getViewName());
        Assert.assertNull(mav.getModel().get("authenticated"));
    }

    @Test
    public void commandNotAuthenticated() throws XMPPException {
        ModelAndView mav = controller.command("foo", session);
        
        Assert.assertEquals("index", mav.getViewName());
        Assert.assertNull(mav.getModel().get("authenticated"));
    }

    @Test
    public void submitCommandNotAuthenticated() throws XMPPException {
        ModelAndView mav = controller.submitCommand("foo", request, session);
        
        Assert.assertEquals("index", mav.getViewName());
        Assert.assertNull(mav.getModel().get("authenticated"));
    }

}
