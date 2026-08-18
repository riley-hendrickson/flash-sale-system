package flashsalesystem.orderservice.dtos;

public record OrderRequest(String productId, int quantity, String orderId, double amountDue)
{
}
