package org.apache.vysper.xmpp.delivery.inbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;

public class RelayResult {
    private List<DeliveryException> processingErrors = new ArrayList<DeliveryException>();

    public RelayResult() {
        // empty
    }

    public RelayResult(DeliveryException processingError) {
        addProcessingError(processingError);
    }

    public void addProcessingError(DeliveryException processingError) {
        processingErrors.add(processingError);
    }

    public List<DeliveryException> getProcessingErrors() {
        return Collections.unmodifiableList(processingErrors);
    }

    public boolean hasProcessingErrors() {
        return processingErrors.size() > 0;
    }
}