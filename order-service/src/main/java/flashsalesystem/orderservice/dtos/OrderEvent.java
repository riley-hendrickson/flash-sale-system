package flashsalesystem.orderservice.dtos;

import flashsalesystem.orderservice.enums.OrderResults;

import java.time.Instant;

public record OrderEvent(String orderId, String productId, int quantity, double amountDue, OrderResults result, Instant timestamp)
{
}
