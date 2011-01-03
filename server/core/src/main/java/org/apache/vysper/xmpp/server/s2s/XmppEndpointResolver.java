package org.apache.vysper.xmpp.server.s2s;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;


public class XmppEndpointResolver {

    private static final int SERVER_PORT = 5269;
    
    public static class ResolvedAddress {
        private InetSocketAddress address;
        private int priority;
        public ResolvedAddress(Name name, int port, int priority) {
            this(name.toString(), port, priority);
        }
        public ResolvedAddress(String name, int port, int priority) {
            this.address = new InetSocketAddress(name, port);
            this.priority = priority;
        }
        public InetSocketAddress getAddress() {
            return address;
        }
        public int getPriority() {
            return priority;
        }
        @Override
        public String toString() {
            return "[address=" + address + ", priority=" + priority + "]";
        }
    }

    public List<ResolvedAddress> resolveXmppServer(String domain) {
        List<ResolvedAddress> addresses = new ArrayList<ResolvedAddress>();
        try {
            Record[] records = new Lookup("_xmpp-server._tcp." + domain, Type.SRV).run();
            if(records != null) {
                for (int i = 0; i < records.length; i++) {
                    SRVRecord srv = (SRVRecord) records[i];
                    addresses.add(new ResolvedAddress(srv.getTarget(), srv.getPort(), srv.getPriority()));
                }
                
                // sort by priority
                Collections.sort(addresses, new Comparator<ResolvedAddress>() {
                    public int compare(ResolvedAddress a1, ResolvedAddress a2) {
                        return a1.getPriority() - a2.getPriority();
                    }
                });
            } else {
                addresses.add(new ResolvedAddress(domain, SERVER_PORT, 0)); 
            }
        } catch (TextParseException e) {
            // ignore
        }
        
        return addresses;
    }
    
    
}
