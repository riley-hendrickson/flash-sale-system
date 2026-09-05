package flashsalesystem.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class PaymentServiceConfig
{
    private final double paymentFailureRate;
    private final double processorFailureRate;

    public PaymentServiceConfig(@Value("${app.payment-failure-rate}") double paymentFailureRate, @Value("${app.processor-failure-rate}") double processorFailureRate)
    {
        this.paymentFailureRate = paymentFailureRate;
        this.processorFailureRate = processorFailureRate;
    }

    public double getPaymentFailureRate()
    {
        return paymentFailureRate;
    }

    public double getProcessorFailureRate()
    {
        return processorFailureRate;
    }
}
