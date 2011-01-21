package org.apache.vysper.xmpp.exemples.embeddedwar;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.vysper.xmpp.server.XMPPServer;

public class StatusServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        out.println("Web application deployed successfully");
        
        XMPPServer server = (XMPPServer) getServletContext().getAttribute("vysper");
        if(server != null) {
            if(server.getServerRuntimeContext() != null) {
                out.println("Vysper started");
            } else {
                out.println("Vysper stopped, check error log");
            }
        } else {
            out.println("Vysper not enabled, check error log");
        }
    }
}
