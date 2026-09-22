package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.enums.PaymentResults;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the {@code @CircuitBreaker} on PaymentServiceClient actually opens
 * after repeated failures and then short-circuits later calls (rejecting
 * them before any network attempt) rather than letting every call retry
 * against a downstream that's already known to be down. Retry is disabled
 * here (max-attempts=1) so each processPayment() call advances the circuit
 * breaker's sliding window by exactly one, keeping the call-count math exact.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "resilience4j.retry.instances.paymentService.max-attempts=1",
        "resilience4j.circuitbreaker.instances.paymentService.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.paymentService.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.paymentService.wait-duration-in-open-state=1m"
})
class PaymentServiceClientCircuitBreakerTest
{
    @DynamicPropertySource
    static void pointAtAnUnreachablePort(DynamicPropertyRegistry registry) throws IOException
    {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0))
        {
            unusedPort = socket.getLocalPort();
        }
        registry.add("services.payment-service.url", () -> "http://localhost:" + unusedPort);
    }

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void processPayment_opensCircuit_andRejectsSubsequentCalls_afterRepeatedFailures()
    {
        // Two failures fill the 2-call sliding window at a 100% failure rate,
        // which exceeds the (default 50%) threshold and opens the circuit.
        paymentServiceClient.processPayment("order-1", 19.98);
        paymentServiceClient.processPayment("order-2", 19.98);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentService");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // The circuit is open, so this call must be rejected immediately -
        // proven by the not-permitted counter, not just the outcome.
        PaymentResults result = paymentServiceClient.processPayment("order-3", 19.98);

        assertThat(result).isEqualTo(PaymentResults.PAYMENT_SERVICE_UNAVAILABLE);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
    }
}
