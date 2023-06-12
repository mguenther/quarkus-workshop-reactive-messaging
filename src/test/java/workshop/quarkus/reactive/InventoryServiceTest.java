package workshop.quarkus.reactive;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import workshop.quarkus.reactive.command.PlaceOrder;
import workshop.quarkus.reactive.event.OrderApprovedEvent;
import workshop.quarkus.reactive.event.OrderDeniedEvent;
import workshop.quarkus.reactive.event.OrderSubmittedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static workshop.quarkus.reactive.TestDataGenerator.toOrderSubmittedEvent;

@QuarkusTest
@QuarkusTestResource(value = InMemoryChannelsTestResource.class)
public class InventoryServiceTest {

    @AfterEach
    void clearChannels() {
        connector.sink("outgoing-checked-orders").clear();
    }

    @Inject
    @Any
    InMemoryConnector connector;

    @Test
    @DisplayName("Task 2.2: For every received order that is admissible, an OrderCheckedEvent should be published")
    void test_task_2_2() {

        InMemorySource<Message<OrderSubmittedEvent>> submittedOrders = connector.source("incoming-orders");
        InMemorySink<OrderApprovedEvent> checkedOrders = connector.sink("outgoing-checked-orders");

        PlaceOrder order = TestDataGenerator.randomOrderGen.sample();
        OrderSubmittedEvent event = toOrderSubmittedEvent(order);
        Message<OrderSubmittedEvent> message = Message.of(event).withMetadata(Metadata.of(new OrderMetadata(order.traceId())));

        submittedOrders.send(message);

        assertThat(checkedOrders.received().size()).isEqualTo(1);

        OrderApprovedEvent approvedEvent = checkedOrders.received().stream().findFirst().orElseThrow().getPayload();

        assertThat(approvedEvent.getItemsAvailable()).isTrue();
    }

    @Test
    @DisplayName("Task 2.3: Any order with unavailable line items should be denied, as indicated by an OrderDeniedEvent")
    void test_task_2_3() {

        InMemorySource<Message<OrderSubmittedEvent>> submittedOrders = connector.source("incoming-orders");
        InMemorySink<OrderDeniedEvent> checkedOrders = connector.sink("outgoing-checked-orders");

        PlaceOrder order = TestDataGenerator.randomUnavailableOrderGen.sample();
        OrderSubmittedEvent event = toOrderSubmittedEvent(order);
        Message<OrderSubmittedEvent> message = Message.of(event).withMetadata(Metadata.of(new OrderMetadata(order.traceId())));

        submittedOrders.send(message);

        assertThat(checkedOrders.received().size()).isEqualTo(1);

        OrderDeniedEvent deniedEvent = checkedOrders.received().stream().findFirst().orElseThrow().getPayload();

        assertThat(deniedEvent.getItemsAvailable()).isFalse();
    }
}
