package workshop.quarkus.reactive.command;

public record PlaceOrder(String orderId, String customerId, String productId, Integer quantity, String traceId) {
}
