package org.apache.vysper.xmpp.delivery;

/**
 * this class is thrown if message has not been delivered and was haned over for offline storage
 */
public class DeliveredToOfflineReceiverException extends DeliveryException {
    public DeliveredToOfflineReceiverException() {
        super();
    }

    public DeliveredToOfflineReceiverException(String string) {
        super(string);
    }

    public DeliveredToOfflineReceiverException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public DeliveredToOfflineReceiverException(Throwable throwable) {
        super(throwable);
    }
}
