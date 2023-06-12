package workshop.quarkus.reactive;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

import java.util.Collections;
import java.util.Map;

public class InMemoryChannelsTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        resetConnectorConfiguration();

        // switch all incoming channels to in-memory-realized channels
        InMemoryConnector.switchIncomingChannelsToInMemory("incoming-orders", "incoming-checked-orders");
        // switch all outgoing channels to in-memory-realized channels
        InMemoryConnector.switchOutgoingChannelsToInMemory("outgoing-orders", "outgoing-checked-orders");

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        resetConnectorConfiguration();
    }

    private void resetConnectorConfiguration() {
        InMemoryConnector.clear();
    }
}
