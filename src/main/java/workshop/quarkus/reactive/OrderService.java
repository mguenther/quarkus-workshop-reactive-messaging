package workshop.quarkus.reactive;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import workshop.quarkus.reactive.command.PlaceOrder;

@ApplicationScoped
public class OrderService {

    public Uni<Void> submitOrder(PlaceOrder order) {
        return Uni.createFrom().voidItem();
    }
}