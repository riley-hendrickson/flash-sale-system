package flashsalesystem.orderservice.dtos;

import flashsalesystem.orderservice.enums.OrderResults;

public record OrderResponse(OrderResults result, String message)
{
}
