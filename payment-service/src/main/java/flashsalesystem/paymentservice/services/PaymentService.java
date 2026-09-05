package flashsalesystem.paymentservice.services;

import flashsalesystem.paymentservice.config.PaymentServiceConfig;
import flashsalesystem.paymentservice.enums.PaymentResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService
{
    private final PaymentServiceConfig paymentServiceConfig;

    public PaymentService(PaymentServiceConfig paymentServiceConfig)
    {
        this.paymentServiceConfig = paymentServiceConfig;
    }

    public PaymentResult processPayment(String orderId, double amountDue)
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() <= paymentServiceConfig.getPaymentFailureRate()) return PaymentResult.PAYMENT_FAILED;
        if (random.nextDouble() <= paymentServiceConfig.getProcessorFailureRate()) return PaymentResult.PROCESSOR_ERROR;

        return PaymentResult.SUCCESS;
    }
}
