package workshop.quarkus.reactive.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class EventDeserializer implements Deserializer<Event> {

    private final ObjectMapper mapper;

    public EventDeserializer() {
        final PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder().build();
        mapper = new ObjectMapper();
        mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL);
    }

    @Override
    public Event deserialize(String s, byte[] bytes) {
        try {
            return mapper.readValue(bytes, Event.class);
        } catch (Exception e) {
            throw new SerializationException("Unable to deserialize record payload into sub-type of the Event protocol.", e);
        }
    }
}
