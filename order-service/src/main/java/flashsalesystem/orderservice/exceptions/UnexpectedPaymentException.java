package flashsalesystem.orderservice.exceptions;

public class UnexpectedPaymentException extends RuntimeException
{
    public UnexpectedPaymentException(String message)
    {
        super(message);
    }
}
