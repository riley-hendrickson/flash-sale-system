package flashsalesystem.paymentservice.dtos;

public record PaymentRequest(String orderId, double amountDue)
{
}
