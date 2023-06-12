package workshop.quarkus.reactive;

import net.mguenther.gen.Gen;
import workshop.quarkus.reactive.command.PlaceOrder;
import workshop.quarkus.reactive.event.OrderApprovedEvent;
import workshop.quarkus.reactive.event.OrderDeniedEvent;
import workshop.quarkus.reactive.event.OrderSubmittedEvent;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static net.mguenther.gen.Gen.choose;
import static net.mguenther.gen.Gen.lift;

public class TestDataGenerator {

    public static Gen<String> idGen = lift(UUID::randomUUID).map(UUID::toString);

    private static Gen<String> traceIdRandomSegmentGen = lift(UUID::randomUUID).map(UUID::toString).map(uuid -> uuid.substring(0, 8));

    public static Gen<String> traceIdForAvailableItemsGen = traceIdRandomSegmentGen.map(id -> "X-" + id);

    public static Gen<String> traceIdForUnavailableItems = traceIdRandomSegmentGen.map(id -> "Y-" + id);

    public static Gen<PlaceOrder> randomOrderGen =
            idGen.flatMap(orderId ->
                    idGen.flatMap(customerId ->
                            idGen.flatMap(productId ->
                                    choose(1, 1_000).flatMap(quantity ->
                                            traceIdForAvailableItemsGen.map(traceId -> new PlaceOrder(orderId, customerId, productId, quantity, traceId))))));

    public static Gen<PlaceOrder> randomUnavailableOrderGen =
            idGen.flatMap(orderId ->
                    idGen.flatMap(customerId ->
                            idGen.flatMap(productId ->
                                    choose(1, 1_000).flatMap(quantity ->
                                            traceIdForUnavailableItems.map(traceId -> new PlaceOrder(orderId, customerId, productId, quantity, traceId))))));

    public static OrderSubmittedEvent toOrderSubmittedEvent(PlaceOrder placeOrder) {
        return new OrderSubmittedEvent(
                placeOrder.orderId(),
                placeOrder.customerId(),
                placeOrder.productId(),
                placeOrder.quantity());
    }

    public static OrderApprovedEvent toOrderApprovedEvent(PlaceOrder placeOrder) {
        return new OrderApprovedEvent(
                placeOrder.orderId(),
                placeOrder.customerId(),
                placeOrder.productId(),
                placeOrder.quantity(),
                true,
                Instant.now(Clock.systemUTC()).toEpochMilli());
    }

    public static OrderDeniedEvent toOrderDeniedEvent(PlaceOrder placeOrder) {
        return new OrderDeniedEvent(
                placeOrder.orderId(),
                placeOrder.customerId(),
                placeOrder.productId(),
                placeOrder.quantity(),
                false,
                Instant.now(Clock.systemUTC()).toEpochMilli());
    }
}
