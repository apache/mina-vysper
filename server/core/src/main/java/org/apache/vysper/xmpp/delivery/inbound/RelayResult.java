package org.apache.vysper.xmpp.delivery.inbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;

public class RelayResult {
    private List<DeliveryException> processingErrors = null;

    private AtomicInteger relayed = new AtomicInteger(0);

    public RelayResult() {
        // empty
    }

    public RelayResult(DeliveryException processingError) {
        addProcessingError(processingError);
    }

    public void addProcessingError(DeliveryException processingError) {
        if (processingError == null)
            processingErrors = new ArrayList<DeliveryException>();
        processingErrors.add(processingError);
    }

    public boolean isRelayed() {
        return relayed.get() > 0;
    }

    public List<DeliveryException> getProcessingErrors() {
        if (processingErrors == null) {
            return Collections.<DeliveryException> emptyList();
        } else {
            return Collections.unmodifiableList(processingErrors);
        }
    }

    public boolean hasProcessingErrors() {
        return processingErrors != null && processingErrors.size() > 0;
    }
}