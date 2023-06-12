package workshop.quarkus.reactive;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import workshop.quarkus.reactive.event.Event;
import workshop.quarkus.reactive.event.OrderApprovedEvent;
import workshop.quarkus.reactive.event.OrderDeniedEvent;

import java.util.HashSet;
import java.util.Set;

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
