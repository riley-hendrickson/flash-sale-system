package flashsalesystem.paymentservice.services;

import flashsalesystem.paymentservice.config.PaymentServiceConfig;
import flashsalesystem.paymentservice.enums.PaymentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest
{
    // ThreadLocalRandom.nextDouble() returns a value in [0.0, 1.0), so a rate
    // of 1.0 always trips the check and a rate of 0.0 practically never does -
    // making the outcome deterministic without needing to inject a Random.

    @Test
    void processPayment_returnsPaymentFailed_whenPaymentFailureRateIsCertain()
    {
        PaymentService paymentService = new PaymentService(new PaymentServiceConfig(1.0, 0.0));

        PaymentResult result = paymentService.processPayment("order-1", 19.98);

        assertThat(result).isEqualTo(PaymentResult.PAYMENT_FAILED);
    }

    @Test
    void processPayment_returnsProcessorError_whenProcessorFailureRateIsCertain()
    {
        PaymentService paymentService = new PaymentService(new PaymentServiceConfig(0.0, 1.0));

        PaymentResult result = paymentService.processPayment("order-1", 19.98);

        assertThat(result).isEqualTo(PaymentResult.PROCESSOR_ERROR);
    }

    @Test
    void processPayment_returnsSuccess_whenBothFailureRatesAreZero()
    {
        PaymentService paymentService = new PaymentService(new PaymentServiceConfig(0.0, 0.0));

        PaymentResult result = paymentService.processPayment("order-1", 19.98);

        assertThat(result).isEqualTo(PaymentResult.SUCCESS);
    }
}
