package workshop.quarkus.reactive;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import workshop.quarkus.reactive.command.PlaceOrder;
import workshop.quarkus.reactive.event.OrderSubmittedEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(value = InMemoryChannelsTestResource.class)
public class OrderServiceTest {

    @AfterEach
    void clearChannels() {
        connector.sink("outgoing-orders").clear();
    }

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    OrderService orderService;

    @Test
    @DisplayName("Task 1.2: Placing an order should generate an OrderSubmittedEvent")
    void test_task_1_2() {
        InMemorySink<OrderSubmittedEvent> orders = connector.sink("outgoing-orders");

        PlaceOrder order = TestDataGenerator.randomOrderGen.sample();

        orderService.submitOrder(order).subscribe().asCompletionStage().join();

        assertThat(orders.received().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Task 1.4: An OrderSubmittedEvent should carry a trace ID")
    void test_task_1_4() {
        InMemorySink<OrderSubmittedEvent> orders = connector.sink("outgoing-orders");

        PlaceOrder order = TestDataGenerator.randomOrderGen.sample();

        orderService.submitOrder(order).subscribe().asCompletionStage().join();

        assertThat(orders.received().size()).isEqualTo(1);

        assertThat(orders.received().stream()
                .map(Message::getMetadata)
                .map(meta -> meta.get(OrderMetadata.class))
                .allMatch(Optional::isPresent)).isTrue();

        assertThat(orders.received().stream()
                .map(Message::getMetadata)
                .flatMap(meta -> meta.get(OrderMetadata.class).stream())
                .allMatch(meta -> meta.traceId() != null)).isTrue();
    }
}
