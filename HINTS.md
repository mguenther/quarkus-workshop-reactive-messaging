# Hints

**Spoiler Alert**

We encourage you to work on the assignment yourself or together with your peers. However, situations may present themselves to you where you're stuck on a specific assignment. Thus, this document contains a couple of hints that ought to guide you through a specific task of the lab assignment.

In any case, don't hesitate to talk to us if you're stuck on a given problem!

## Task 1.1

`@Outgoing` only works on methods that do not consume any method arguments or methods that are explicitly annotated with `@Incoming`. Trying to do so results in an error:

```plain
java.lang.RuntimeException: java.lang.RuntimeException: io.quarkus.builder.BuildException: Build failure: Build failed due to errors
[error]: Build step io.quarkus.smallrye.reactivemessaging.deployment.SmallRyeReactiveMessagingProcessor#build threw an exception: jakarta.enterprise.inject.spi.DefinitionException: SRMSG00053: Invalid method annotated with @Outgoing: workshop.quarkus.reactive.OrderService#submitOrder - no parameters expected
<stacktrace omitted>
```

You could also argue that, from a point-of-view of your tests, Reactive Messaging isn't calling the `OrderService`, but our test cases. So the whole integration using `@Outgoing` can only work if SmallRye hooks everything up internally in order to manage the subscription properly. 

## Task 1.2

You can either work directly on payload type `T` or on the `Message<T>` abstraction. In the first case, the following solution would satisfy the test cases:

```java
@ApplicationScoped
public class OrderService {

    @Channel("outgoing-orders")
    MutinyEmitter<OrderSubmittedEvent> orderChannel;

    public Uni<Void> submitOrder(PlaceOrder order) {
        return Uni.createFrom().item(order)
                .map(o -> new OrderSubmittedEvent(o.orderId(), o.customerId(), o.productId(), o.quantity()))
                .flatMap(orderChannel::send);
    }
}
```

A potential solution that works on `Message<T>` looks like this:

```java
@ApplicationScoped
public class OrderService {

    @Channel("outgoing-orders")
    MutinyEmitter<OrderSubmittedEvent> orderChannel;

    public Uni<Void> submitOrder(PlaceOrder order) {
        return Uni.createFrom().item(order)
                .map(o -> new OrderSubmittedEvent(o.orderId(), o.customerId(), o.productId(), o.quantity()))
                .map(Message::of)
                .flatMap(orderChannel::sendMessage);
    }
}
```

## Task 1.3

While the channel `outgoing-orders` is bound correctly to the `MutinyEmitter<OrderSubmittedEvent>`, there is no logic that integrates this emitter with the actual event publication. Hence, nothing will be published to channel.

## Task 1.4

The way metadata works with SmallRye Reactive Messaging is quite simple: You do not to have a container class or record that carries the metadata. This container should be immutable. Prefer to use Java records in this case, as they cannot be mutated after the fact. You can then use `Metadata.of(Object... metadata)` to associate your metadata container with the `Message`. Accessing metadata is simple as well: Use `Message.getMetadata().get(<type of the container class>)` to get an `Optional` that holds the metadata container (if present).

```java
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
```

## Task 1.5

For one thing, metadata is not stored as key-value-pairs, so other than the type of the class associated with the actual metadata, there is no way of identifying metadata by name. Using `Map<String, String>` is of course possible, but be aware that `Map<String, String>` is a mutable data structure and that you cannot have two `Map`s (regardless of their parameterized types) in the metadata. It is recommended to use an immutable data structure for your metadata that returns attributes by name (for instance by using getters). Hence, a container class, preferably structured around a Java record, is the recommendation.

By the way, Reactive Messaging figures that along the same reactive pipeline, we'd want to relay all metadata along that pipeline. So, the metadata is carried over from `Message` to `Message` across all processing steps - unless manipulated in a way that states otherwise.

## Task 2.1

The Reactive Messaging specification does not allow to use a channel as an ingress and egress channel at the same time. You'd have to use different channels for this. If you're using a messaging backend like Kafka, you're still able to bind dedicated ingress and egress channels to the same topic. The In-Memory Connector is not capable of doing this, however, which limits its suitability for testing drastically, as we cannot test the whole pipeline (one components egress is another components ingress, and we don't have the backing solution to bind them to the same topic).

## Task 2.2

A possible solution is this:

```java
@ApplicationScoped
public class InventoryService {

    @Incoming("incoming-orders")
    @Outgoing("outgoing-checked-orders")
    public Uni<OrderApprovedEvent> checkAvailability(OrderSubmittedEvent event) {
        return Uni.createFrom().item(event)
                .map(e -> new OrderCheckedEvent(
                        e.getOrderId(),
                        e.getCustomerId(),
                        e.getProductId(),
                        e.getQuantity(),
                        true,
                        Instant.now(Clock.systemUTC()).toEpochMilli()));
    }
}
```

## Task 2.3

In order to work with the trace ID, we need access to the metadata of the `Message`. Hence, depending on how you've implemented the previous tasks, you'll have to modify your code so that the `checkAvailability` method consumes a `Message<OrderSubmittedEvent>` rather than the payload type `OrderSubmittedEvent` directly. A possible solution might look like this:

```java
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
```

As you're working with `Message<T>`, you must ack or nack the message explicitly. If you forget to do so, the resp. test case won't complete.

## Task 3.1

Quarkus won't start up. Here is the error message that it throws:

```plain
Caused by: java.lang.RuntimeException: Failed to start quarkus
	at io.quarkus.runner.ApplicationImpl.doStart(Unknown Source)
	at io.quarkus.runtime.Application.start(Application.java:101)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:578)
	at io.quarkus.runner.bootstrap.StartupActionImpl.run(StartupActionImpl.java:273)
	at io.quarkus.test.junit.QuarkusTestExtension.doJavaStart(QuarkusTestExtension.java:251)
	at io.quarkus.test.junit.QuarkusTestExtension.ensureStarted(QuarkusTestExtension.java:607)
	at io.quarkus.test.junit.QuarkusTestExtension.beforeAll(QuarkusTestExtension.java:655)
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeBeforeAllCallbacks$12(ClassBasedTestDescriptor.java:395)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeBeforeAllCallbacks(ClassBasedTestDescriptor.java:395)
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.before(ClassBasedTestDescriptor.java:211)
	... 38 more
Caused by: jakarta.enterprise.inject.spi.DeploymentException: Wiring error(s) detected in application.
	at io.smallrye.reactive.messaging.providers.extension.MediatorManager.start(MediatorManager.java:212)
	at io.smallrye.reactive.messaging.providers.extension.MediatorManager_ClientProxy.start(Unknown Source)
	at io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle.onApplicationStart(SmallRyeReactiveMessagingLifecycle.java:52)
	at io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle_Observer_onApplicationStart_68e7b57eb97cb75d597c5b816682366e888d0d9b.notify(Unknown Source)
	at io.quarkus.arc.impl.EventImpl$Notifier.notifyObservers(EventImpl.java:346)
	at io.quarkus.arc.impl.EventImpl$Notifier.notify(EventImpl.java:328)
	at io.quarkus.arc.impl.EventImpl.fire(EventImpl.java:82)
	at io.quarkus.arc.runtime.ArcRecorder.fireLifecycleEvent(ArcRecorder.java:155)
	at io.quarkus.arc.runtime.ArcRecorder.handleLifecycleEvents(ArcRecorder.java:106)
	at io.quarkus.deployment.steps.LifecycleEventsBuildStep$startupEvent1144526294.deploy_0(Unknown Source)
	at io.quarkus.deployment.steps.LifecycleEventsBuildStep$startupEvent1144526294.deploy(Unknown Source)
	... 50 more
	Suppressed: io.smallrye.reactive.messaging.providers.wiring.TooManyDownstreamCandidatesException: 'ProcessingMethod{method:'checkAvailability', incoming:'orders', outgoing:'checked-orders'}' supports a single downstream consumer, but found 2: [SubscriberMethod{method:'notifyOn', incoming:'checked-orders'}, SubscriberMethod{method:'notifyOn', incoming:'checked-orders'}]. You may want to enable broadcast using '@Broadcast' on the method ProcessingMethod{method:'checkAvailability', incoming:'orders', outgoing:'checked-orders'}.
		at io.smallrye.reactive.messaging.providers.wiring.Wiring$ProcessorMediatorComponent.validate(Wiring.java:826)
		at io.smallrye.reactive.messaging.providers.wiring.Graph.<init>(Graph.java:67)
		at io.smallrye.reactive.messaging.providers.wiring.Wiring.resolve(Wiring.java:175)
		at io.smallrye.reactive.messaging.providers.wiring.Wiring_ClientProxy.resolve(Unknown Source)
		at io.smallrye.reactive.messaging.providers.extension.MediatorManager.start(MediatorManager.java:209)
		... 60 more
```

The last part is interesting: We can only have a single downstream candidate, unless stated otherwise. You might have run into this problem while working on these tasks. Quarkus suggests a fix for this, but be careful as this might change the semantics of your application. If you happen to have multiple downstream subscribers use `@Broadcast` to submit the event to all of them. If you happen to have multiple upstream publishers for a single downstream subscriber, you can use `@Merge` to get all the events from them.

## Task 3.2

Ours is the case for multiple downstream subscribers that will all see _the same data_ if we use `@Broadcast` on an upstream publisher. Hence, won't be able to work on the actual types (cf. `OrderApprovedEvent` and `OrderDeniedEvent`) unless we've specifically casted them into the proper type (before filtering all unwantend ones). So, the recommended solution is to work on the base type `Event`, check on the actual type of the incoming event and handle the events by delegating them to specific event handling methods.

## Task 3.3 / Task 3.4

A possible solution is the following:

```java
@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    private final Set<String> approvedOrdersByOrderId = new HashSet<>();

    private final Set<String> deniedOrdersByOrderId = new HashSet<>();

    @Incoming("incoming-checked-orders")
    public void notify(Event event) {
        if (event instanceof OrderApprovedEvent) {
            onOrderApproved((OrderApprovedEvent) event);
        } else if (event instanceof OrderDeniedEvent) {
            onOrderDenied((OrderDeniedEvent) event);
        }
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

## Task 3.5

Message linking has to be introduced in our intermediary processor, the `InventoryService`. A possible solutions is the following:

```java
@ApplicationScoped
public class InventoryService {

    @Inject
    InventoryAvailabilityChecker checker;

    @Incoming("incoming-orders")
    @Outgoing("outgoing-checked-orders")
    public Message<Event> checkAvailability(Message<OrderSubmittedEvent> message) {

        var e = message.getPayload();
        var traceId = message.getMetadata(OrderMetadata.class).map(OrderMetadata::traceId).orElse("no-trace-id");
        var available = checker.allLineItemsAvailable(traceId);

        return available ?
                message.withPayload(
                        new OrderApprovedEvent(
                                e.getOrderId(),
                                e.getCustomerId(),
                                e.getProductId(),
                                e.getQuantity(),
                                true,
                                Instant.now(Clock.systemUTC()).toEpochMilli())) :
                message.withPayload(
                        new OrderDeniedEvent(
                                e.getOrderId(),
                                e.getCustomerId(),
                                e.getProductId(),
                                e.getQuantity(),
                                false,
                                Instant.now(Clock.systemUTC()).toEpochMilli()));
    }
}
```

Note that message linking carries over the `ack` and `nack` functions.