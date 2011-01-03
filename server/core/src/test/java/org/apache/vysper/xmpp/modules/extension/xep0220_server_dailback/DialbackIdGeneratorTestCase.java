package org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback;
import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0220_server_dailback.DialbackIdGenerator;


public class DialbackIdGeneratorTestCase extends TestCase {

    private Entity receiving = EntityImpl.parseUnchecked("xmpp.example.com");
    private Entity originating = EntityImpl.parseUnchecked("example.org");
    private String streamId = "D60000229F";
    
    public void testId() {
        DialbackIdGenerator generator = new DialbackIdGenerator();
        String id = generator.generate(receiving, originating, streamId);
        
        Assert.assertTrue(generator.verify(id, receiving, originating, streamId));
        
    }

    public void testNotValidId() {
        DialbackIdGenerator generator = new DialbackIdGenerator();
        Assert.assertFalse(generator.verify("1234567890", receiving, originating, streamId));
    }
}
