package flashsalesystem.orderservice.exceptions;

public class PaymentProcessorException extends RuntimeException
{
    public PaymentProcessorException(String message)
    {
        super(message);
    }
}
