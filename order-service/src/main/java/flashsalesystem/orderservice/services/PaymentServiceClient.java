package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.dtos.PaymentRequest;
import flashsalesystem.orderservice.enums.PaymentResults;
import flashsalesystem.orderservice.exceptions.PaymentProcessorException;
import flashsalesystem.orderservice.exceptions.UnexpectedPaymentException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentServiceClient
{
    private final RestClient paymentServiceRestClient;

    public PaymentServiceClient(RestClient paymentServiceRestClient)
    {
        this.paymentServiceRestClient = paymentServiceRestClient;
    }

    @Retry(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public PaymentResults processPayment(String orderId, double amountDue)
    {
        System.out.println("processing payment for order: " + orderId + " with amount: " + amountDue);
        return paymentServiceRestClient.post()
                .uri("/payments")
                .body(new PaymentRequest(orderId, amountDue))
                .exchange((request, response) ->
                {
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.OK)) return PaymentResults.SUCCESS;
                    else if (response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT))
                        return PaymentResults.PAYMENT_FAILED;
                    else if (response.getStatusCode().isSameCodeAs(HttpStatus.BAD_GATEWAY))
                    {
                        throw new PaymentProcessorException("Payment processor error");
                    } else
                    {
                        throw new UnexpectedPaymentException("Unexpected payment error");
                    }
                });
    }

    public PaymentResults processPaymentFallback(String orderId, double amountDue, Throwable exception)
    {
        if(exception instanceof PaymentProcessorException) return PaymentResults.PROCESSOR_ERROR;
        else if(exception instanceof CallNotPermittedException) return PaymentResults.PAYMENT_SERVICE_UNAVAILABLE;
        else return PaymentResults.UNKNOWN_ERROR;
    }
}
