package workshop.quarkus.reactive;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InventoryAvailabilityChecker {

    public boolean allLineItemsAvailable(final String traceId) {
        return traceId.startsWith("X-");
    }
}
