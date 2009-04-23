package org.apache.vysper.xmpp.addressing.stringprep;

import org.apache.vysper.xmpp.addressing.EntityFormatException;

/**
 * @author Gerolf Seitz (gseitz@apache.org)
 *
 */
public class StringPrepViolationException extends EntityFormatException {

	private static final long serialVersionUID = 1L;

	public StringPrepViolationException() {
	}
	
	public StringPrepViolationException(String message) {
		super(message);
	}
	
	
	
}
