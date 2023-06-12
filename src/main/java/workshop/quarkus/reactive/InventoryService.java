package workshop.quarkus.reactive;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import workshop.quarkus.reactive.event.OrderApprovedEvent;
import workshop.quarkus.reactive.event.OrderSubmittedEvent;

@ApplicationScoped
public class InventoryService {

    @Incoming("incoming-orders")
    @Outgoing("outgoing-checked-orders")
    public Uni<OrderApprovedEvent> checkAvailability(OrderSubmittedEvent event) {
        return Uni.createFrom().nullItem();
    }
}