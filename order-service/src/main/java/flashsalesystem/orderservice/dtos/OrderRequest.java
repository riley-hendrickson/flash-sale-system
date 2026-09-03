package flashsalesystem.orderservice.dtos;

public record OrderRequest(Long productId, int quantity, String orderId, double amountDue)
{
}
