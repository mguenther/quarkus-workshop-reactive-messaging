package workshop.quarkus.reactive;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import workshop.quarkus.reactive.command.PlaceOrder;
import workshop.quarkus.reactive.event.Event;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(value = InMemoryChannelsTestResource.class)
public class NotificationServiceTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    NotificationService notificationService;

    @Test
    @DisplayName("Task 3.3: Signal an OrderApprovedEvent to the user")
    void test_3_3_available() {

        InMemorySource<Event> checkedOrders = connector.source("incoming-checked-orders");

        PlaceOrder order = TestDataGenerator.randomOrderGen.sample();
        Event event = TestDataGenerator.toOrderApprovedEvent(order);

        checkedOrders.send(event);

        assertThat(notificationService.isApproved(order.orderId())).isTrue();
    }

    @Test
    @DisplayName("Task 3.4: Signal an OrderDeniedEvent to the user")
    void test_3_4_unavailable() {

        InMemorySource<Event> checkedOrders = connector.source("incoming-checked-orders");

        PlaceOrder order = TestDataGenerator.randomUnavailableOrderGen.sample();
        Event event = TestDataGenerator.toOrderDeniedEvent(order);

        checkedOrders.send(event);

        assertThat(notificationService.isDenied(order.orderId())).isTrue();
    }
}
