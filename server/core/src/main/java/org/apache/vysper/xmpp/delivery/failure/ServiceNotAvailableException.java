package org.apache.vysper.xmpp.delivery.failure;

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;

/**
 */
public class ServiceNotAvailableException extends DeliveryException {
    public ServiceNotAvailableException() {
        super();
    }

    public ServiceNotAvailableException(String string) {
        super(string);
    }

    public ServiceNotAvailableException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public ServiceNotAvailableException(Throwable throwable) {
        super(throwable);
    }
}
