package workshop.quarkus.reactive;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import workshop.quarkus.reactive.event.Event;
import workshop.quarkus.reactive.event.OrderApprovedEvent;
import workshop.quarkus.reactive.event.OrderDeniedEvent;
import workshop.quarkus.reactive.event.OrderSubmittedEvent;

import java.time.Clock;
import java.time.Instant;

@ApplicationScoped
public class InventoryService {

    @Inject
    InventoryAvailabilityChecker checker;

    @Incoming("incoming-orders")
    @Outgoing("outgoing-checked-orders")
    public Event checkAvailability(Message<OrderSubmittedEvent> message) {

        var e = message.getPayload();
        var traceId = message.getMetadata(OrderMetadata.class).map(OrderMetadata::traceId).orElse("no-trace-id");
        var available = checker.allLineItemsAvailable(traceId);

        try {
            return available ?
                    new OrderApprovedEvent(
                            e.getOrderId(),
                            e.getCustomerId(),
                            e.getProductId(),
                            e.getQuantity(),
                            true,
                            Instant.now(Clock.systemUTC()).toEpochMilli()) :
                    new OrderDeniedEvent(
                            e.getOrderId(),
                            e.getCustomerId(),
                            e.getProductId(),
                            e.getQuantity(),
                            false,
                            Instant.now(Clock.systemUTC()).toEpochMilli());
        } finally {
            message.ack();
        }
    }
}