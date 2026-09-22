package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.enums.PaymentResults;
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
 * Proves {@code @Retry} on PaymentServiceClient actually makes multiple
 * attempts before giving up, not just that it eventually falls back (that
 * alone would also be true with zero retries). Since Retry wraps
 * CircuitBreaker by default (resilience4j's default aspect order), every
 * retry attempt is individually recorded by the circuit breaker - so the
 * circuit breaker's own failed-call count is repurposed here as a precise,
 * built-in attempt counter instead of standing up a fake server to count
 * connections. The circuit breaker's window is kept large so this test's
 * failures never trip it open; that's PaymentServiceClientCircuitBreakerTest's
 * job.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "resilience4j.retry.instances.paymentService.max-attempts=3",
        "resilience4j.retry.instances.paymentService.wait-duration=10ms",
        "resilience4j.circuitbreaker.instances.paymentService.sliding-window-size=100",
        "resilience4j.circuitbreaker.instances.paymentService.minimum-number-of-calls=100"
})
class PaymentServiceClientRetryTest
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
    void processPayment_retriesConfiguredAttempts_beforeFallingBack()
    {
        PaymentResults result = paymentServiceClient.processPayment("order-1", 19.98);

        assertThat(result).isEqualTo(PaymentResults.UNKNOWN_ERROR);
        assertThat(circuitBreakerRegistry.circuitBreaker("paymentService").getMetrics().getNumberOfFailedCalls())
                .isEqualTo(3);
    }
}
