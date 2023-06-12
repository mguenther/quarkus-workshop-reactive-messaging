# Lab Assignment

## Task #1: Producing messages

Have a look at class `OrderService`. Its implementation provides the bare minimum to work on.

```java
@ApplicationScoped
public class OrderService {

    public Uni<Void> submitOrder(PlaceOrder order) {
        return Uni.createFrom().voidItem();
    }
}
```

There is a set of test cases in class `OrderServiceTest`. Running these tests fails given the incomplete implementation of `OrderService`. Your job is to make these tests pass. Use the test cases to track your progress when working through this task.

1. Why can't we simply annotate the method `submitOrder` with `@Outgoing`? What happens if we do so?

2. Consume the `PlaceOrder` command and transform it into an instance of `OrderSubmittedEvent`. Publish this event to the channel called `outgoing-orders`.

3. What can you say about the following solution?

```java
@ApplicationScoped
public class OrderService {

    @Channel("outgoing-orders")
    MutinyEmitter<OrderSubmittedEvent> orderChannel;

    public Uni<Void> submitOrder(PlaceOrder order) {
        return Uni.createFrom().item(order)
                .map(o -> new OrderSubmittedEvent(o.orderId(), o.customerId(), o.productId(), o.quantity()))
				.replaceWithVoid();
    }
}
```

4. To ensure traceability of orders, we'd like to add a trace ID to the metadata of the message that wraps an `OrderSubmittedEvent`. A trace ID is already generated for you (cf. `PlaceOrder` command). Use the record `OrderMetadata` as metadata backend.

5. Why do we have to use a container class like `OrderMetadata`? What happens if you try to associate two distinct metadata attributes to the `Message` that have the same Java type?

## Task #2: Processing messages

We're now going to implement a stream processor that consumes `OrderSubmittedEvent`s and performs an availability check for all line items comprising the order. If all line items are available, then the order process can proceed. We'll indicate by publishing an `OrderApprovedEvent` to channel `outgoing-checked-orders`.

Have a look at class `InventoryService`.

```java
@ApplicationScoped
public class InventoryService {

    @Incoming("incoming-orders")
    @Outgoing("outgoing-checked-orders")
    public Uni<OrderApprovedEvent> checkAvailability(OrderSubmittedEvent event) {
        return Uni.createFrom().nullItem();
    }
}
```

1. Why can't we use `@Incoming("outgoing-orders")`?

2. Obviously, the current implementation is missing a few key ingredients. Your job for this task is to implement a simple relay: Consume the `OrderSubmittedEvent` and transform the data into an `OrderApprovedEvent` and publish it.

3. We're now going to check if an order can be fulfilled. Copy the following code into your implementation of `InventoryService`.

```java
@ApplicationScoped
public class InventoryService {

    @Inject
    InventoryAvailabilityChecker checker;

    /* rest of your implementation */
}
```

The `InventoryAvailabilityChecker` decides whether an order can be fulfilled due to the characteristics of the trace ID. If the trace ID starts with an `X-`, it assumes that all line items are available. If it starts with something else, then it must assume that line items are out of stock and process the event as appropriate.

In case all line items are available, we'll still publish the `OrderApprovedEvent`. In case some line items are not available, we'll publish an `OrderDeniedEvent` event. The outgoing channel remains the same for both event types.

## Task #3: Consuming messages (of different types)

We're now shifting our attention to the final downstream subscriber of our pipeline: A simple notification service that informs the customer whether her order has been approved. Have a look at the skeleton for class `NotificationService`.

```java
@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    private final Set<String> approvedOrdersByOrderId = new HashSet<>();

    private final Set<String> deniedOrdersByOrderId = new HashSet<>();

    @Incoming("incoming-checked-orders")
    public void notify(Event event) {
    }

    private void onOrderApproved(final OrderApprovedEvent event) {
        LOG.info("Received an OrderApprovedEvent for order with ID " + event.getOrderId());
        approvedOrdersByOrderId.add(event.getOrderId());
    }

    private void onOrderDenied(final OrderDeniedEvent event) {
        LOG.info("Received an OrderDeniedEvent for order with ID " + event.getOrderId());
        deniedOrdersByOrderId.add(event.getOrderId());
    }

    public boolean isApproved(final String orderId) {
        return approvedOrdersByOrderId.contains(orderId);
    }

    public boolean isDenied(final String orderId) {
        return deniedOrdersByOrderId.contains(orderId);
    }
}
```

1. We're interested in handling `OrderApprovedEvent`s and `OrderDeniedEvent`s. Why can't we simply write the following two methods? How does Quarkus react?

```java
@Incoming("incoming-checked-orders")
public void notifyOn(OrderApprovedEvent event) {
}

@Incoming("incoming-checked-orders")
public void notifyOn(OrderDeniedEvent event) {
}
```

2. How can we fix the issue?

3. Implement either approach for `OrderApprovedEvent`s.

4. Implement either approach for `OrderDeniedEvent`s.

5. We talked about the concept of linking messages together to build an acknowledgement chain. Link the `OrderSubmittedEvent` with the `OrderApprovedEvent` and `OrderDeniedEvent` respectively.

## That's it! You've done great!

You have completed all assignments. If you have any further questions or need clarification, please don't hesitate to reach out to us. We're here to help.