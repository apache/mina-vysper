package org.apache.vysper.xmpp.delivery.inbound;

import java.util.List;

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.stanza.Stanza;

public class RecordingDeliveryFailureStrategy implements DeliveryFailureStrategy {

    private Stanza recordedStanza;

    private List<DeliveryException> recordedDeliveryException;

    public void process(Stanza failedToDeliverStanza, List<DeliveryException> deliveryException)
            throws DeliveryException {
        this.recordedStanza = failedToDeliverStanza;
        this.recordedDeliveryException = deliveryException;
    }

    public Stanza getRecordedStanza() {
        return recordedStanza;
    }

    public List<DeliveryException> getRecordedDeliveryException() {
        return recordedDeliveryException;
    }
}