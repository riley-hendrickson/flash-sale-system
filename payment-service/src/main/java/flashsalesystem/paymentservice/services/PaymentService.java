package flashsalesystem.paymentservice.services;

import flashsalesystem.paymentservice.enums.PaymentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService
{
    private final double paymentFailureRate;
    private final double processorFailureRate;

    public PaymentService(@Value("${app.payment-failure-rate}") double paymentFailureRate, @Value("${app.processor-failure-rate}") double processorFailureRate)
    {
        this.paymentFailureRate = paymentFailureRate;
        this.processorFailureRate = processorFailureRate;
    }

    public PaymentResult processPayment(String orderId, double amountDue)
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if(random.nextDouble() <= paymentFailureRate) return PaymentResult.PAYMENT_FAILED;
        if(random.nextDouble() <= processorFailureRate) return PaymentResult.PROCESSOR_ERROR;

        return PaymentResult.SUCCESS;
    }
}
