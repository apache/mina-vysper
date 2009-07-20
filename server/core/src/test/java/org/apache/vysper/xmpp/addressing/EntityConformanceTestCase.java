package org.apache.vysper.xmpp.addressing;

import junit.framework.TestCase;

import java.util.Arrays;

public class EntityConformanceTestCase extends TestCase {

	public void testCheckRFC3920Conformance() {
		String error = buildLargeString(1024);
		String okButOnTheEdge = buildLargeString(1023);
        runAllChecks(error, "x");
        runAllChecks(error, okButOnTheEdge);
    }

    private void runAllChecks(String error, String ok) {

        assertFalse(doCheck(error, ok, ok));
        assertFalse(doCheck(ok, error, ok));
        assertFalse(doCheck(ok, ok, error));
        assertFalse(doCheck(ok, null, ok));
        assertFalse(doCheck(ok, "", ok));

        assertTrue(doCheck(ok, ok, ok));
        assertTrue(doCheck(null, ok, null));
        assertTrue(doCheck(ok, ok, null));
        assertTrue(doCheck(null, ok, ok));
        assertTrue(doCheck("", ok, ""));
        assertTrue(doCheck(ok, ok, ""));
        assertTrue(doCheck("", ok, ok));
    }

    private boolean doCheck(String node, String domain, String resource) {
		return EntityConformance.checkRFC3920Conformance(new EntityImpl(
				node, domain, resource));
	}

	private String buildLargeString(int length) {
		char[] chars = new char[length];
		Arrays.fill(chars, 'x');
		return new String(chars);
	}

    public void testEquals() {
        assertEquals(new EntityImpl(null, "vysper.org", null), new EntityImpl(null, "vysper.org", ""));
        assertEquals(new EntityImpl(null, "vysper.org", null), new EntityImpl("", "vysper.org", null));
        assertEquals(new EntityImpl(null, "vysper.org", null), new EntityImpl("", "vysper.org", ""));
    }
}
