package workshop.quarkus.reactive;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import workshop.quarkus.reactive.command.PlaceOrder;
import workshop.quarkus.reactive.event.OrderSubmittedEvent;

@ApplicationScoped
public class OrderService {

    @Channel("outgoing-orders")
    MutinyEmitter<OrderSubmittedEvent> orderChannel;

    public Uni<Void> submitOrder(PlaceOrder order) {

        return Uni.createFrom().item(order)
                .map(o -> new OrderSubmittedEvent(o.orderId(), o.customerId(), o.productId(), o.quantity()))
                .map(Message::of)
                .map(m -> m.withMetadata(Metadata.of(new OrderMetadata(order.traceId()))))
                .flatMap(orderChannel::sendMessage);
    }
}