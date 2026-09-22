package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.enums.PaymentResults;
import flashsalesystem.orderservice.exceptions.PaymentProcessorException;
import flashsalesystem.orderservice.exceptions.UnexpectedPaymentException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PaymentServiceClientTest
{
    private MockRestServiceServer server;
    private PaymentServiceClient client;

    @BeforeEach
    void setUp()
    {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://payment-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PaymentServiceClient(builder.build());
    }

    @Test
    void processPayment_returnsSuccess_on200()
    {
        server.expect(requestTo("http://payment-service/payments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        PaymentResults result = client.processPayment("order-1", 19.98);

        assertThat(result).isEqualTo(PaymentResults.SUCCESS);
        server.verify();
    }

    @Test
    void processPayment_returnsPaymentFailed_on409()
    {
        server.expect(requestTo("http://payment-service/payments"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        PaymentResults result = client.processPayment("order-1", 19.98);

        assertThat(result).isEqualTo(PaymentResults.PAYMENT_FAILED);
    }

    @Test
    void processPayment_throwsPaymentProcessorException_on502()
    {
        server.expect(requestTo("http://payment-service/payments"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.processPayment("order-1", 19.98))
                .isInstanceOf(PaymentProcessorException.class);
    }

    @Test
    void processPayment_throwsUnexpectedPaymentException_onUnrecognizedStatus()
    {
        server.expect(requestTo("http://payment-service/payments"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.processPayment("order-1", 19.98))
                .isInstanceOf(UnexpectedPaymentException.class);
    }

    @Test
    void processPaymentFallback_returnsProcessorError_whenCauseIsPaymentProcessorException()
    {
        PaymentResults result = client.processPaymentFallback("order-1", 19.98, new PaymentProcessorException("down"));

        assertThat(result).isEqualTo(PaymentResults.PROCESSOR_ERROR);
    }

    @Test
    void processPaymentFallback_returnsServiceUnavailable_whenCircuitBreakerRejectsCall()
    {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("paymentService");
        CallNotPermittedException exception = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        PaymentResults result = client.processPaymentFallback("order-1", 19.98, exception);

        assertThat(result).isEqualTo(PaymentResults.PAYMENT_SERVICE_UNAVAILABLE);
    }

    @Test
    void processPaymentFallback_returnsUnknownError_whenCauseIsAnythingElse()
    {
        PaymentResults result = client.processPaymentFallback("order-1", 19.98, new RuntimeException("boom"));

        assertThat(result).isEqualTo(PaymentResults.UNKNOWN_ERROR);
    }
}
