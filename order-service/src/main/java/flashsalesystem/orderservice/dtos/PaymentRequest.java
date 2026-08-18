package flashsalesystem.orderservice.dtos;

public record PaymentRequest(String orderId, double amountDue)
{
}
