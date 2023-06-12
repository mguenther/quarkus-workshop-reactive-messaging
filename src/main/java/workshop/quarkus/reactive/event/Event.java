package workshop.quarkus.reactive.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderSubmittedEvent.class, name = "order-submitted-event"),
        @JsonSubTypes.Type(value = OrderApprovedEvent.class, name = "order-approved-event"),
        @JsonSubTypes.Type(value = OrderDeniedEvent.class, name = "order-denied-event")
})
abstract public class Event {

    private final String eventId;

    private final long eventTime;

    public Event() {
        this(generateEventId(), now());
    }

    public Event(@JsonProperty("eventId") final String eventId,
                 @JsonProperty("eventTime") final long eventTime) {
        this.eventId = eventId;
        this.eventTime = eventTime;
    }

    public String getEventId() {
        return eventId;
    }

    public long getEventTime() {
        return eventTime;
    }

    private static String generateEventId() {
        return UUID.randomUUID().toString();
    }

    private static long now() {
        return Instant.now(Clock.systemUTC()).toEpochMilli();
    }
}
