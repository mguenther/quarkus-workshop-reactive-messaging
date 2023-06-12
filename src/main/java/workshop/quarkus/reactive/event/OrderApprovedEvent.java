package workshop.quarkus.reactive.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderApprovedEvent extends Event {

    private final String orderId;

    private final String customerId;

    private final String productId;

    private final Integer quantity;

    private final Boolean itemsAvailable;

    private final Long checkedOn;

    @JsonCreator
    public OrderApprovedEvent(@JsonProperty("orderId") final String orderId,
                              @JsonProperty("customerId") final String customerId,
                              @JsonProperty("productId") final String productId,
                              @JsonProperty("quantity") final Integer quantity,
                              @JsonProperty("itemsAvailable") final Boolean itemsAvailable,
                              @JsonProperty("checkedOn") final Long checkedOn) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.itemsAvailable = itemsAvailable;
        this.checkedOn = checkedOn;
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

    public Boolean getItemsAvailable() {
        return itemsAvailable;
    }

    public Long getCheckedOn() {
        return checkedOn;
    }
}
