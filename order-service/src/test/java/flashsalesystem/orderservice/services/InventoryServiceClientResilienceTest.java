package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.enums.ReservationResults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the {@code @Retry}/{@code @CircuitBreaker} AOP wiring on
 * InventoryServiceClient actually activates and degrades to the fallback,
 * rather than letting a real connection failure propagate as an exception.
 * Unlike InventoryServiceClientTest, this exercises the Spring-proxied bean,
 * not a bare instance, since the resilience4j annotations are no-ops without it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "resilience4j.retry.instances.inventoryService.max-attempts=2",
        "resilience4j.retry.instances.inventoryService.wait-duration=10ms"
})
class InventoryServiceClientResilienceTest
{
    @DynamicPropertySource
    static void pointAtAnUnreachablePort(DynamicPropertyRegistry registry) throws IOException
    {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0))
        {
            unusedPort = socket.getLocalPort();
        }
        registry.add("services.inventory-service.url", () -> "http://localhost:" + unusedPort);
    }

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Test
    void reserveStock_fallsBackToServiceUnavailable_whenInventoryServiceIsUnreachable()
    {
        ReservationResults result = inventoryServiceClient.reserveStock(1L, 1);

        assertThat(result).isEqualTo(ReservationResults.INVENTORY_SERVICE_UNAVAILABLE);
    }
}
