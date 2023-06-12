package workshop.quarkus.reactive.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderSubmittedEvent extends Event {

    private final String orderId;

    private final String customerId;

    private final String productId;

    private final Integer quantity;

    @JsonCreator
    public OrderSubmittedEvent(@JsonProperty("orderId") final String orderId,
                               @JsonProperty("customerId") final String customerId,
                               @JsonProperty("productId") final String productId,
                               @JsonProperty("quantity") final Integer quantity) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
