package flashsalesystem.notificationservice.events;

import flashsalesystem.notificationservice.enums.OrderResults;

import java.time.Instant;

public record OrderEvent(String orderId, String productId, int quantity, double amountDue, OrderResults result, Instant timestamp)
{
}
