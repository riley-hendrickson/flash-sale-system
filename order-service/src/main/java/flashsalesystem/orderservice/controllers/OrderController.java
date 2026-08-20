package flashsalesystem.orderservice.controllers;

import flashsalesystem.orderservice.dtos.OrderRequest;
import flashsalesystem.orderservice.dtos.OrderResponse;
import flashsalesystem.orderservice.enums.OrderResults;
import flashsalesystem.orderservice.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController
{
    private final OrderService orderService;

    public OrderController(OrderService orderService)
    {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest orderRequest)
    {
        OrderResults result = orderService.placeOrder(orderRequest.productId(), orderRequest.quantity(), orderRequest.orderId(), orderRequest.amountDue());

        if(result == OrderResults.SUCCESS) return ResponseEntity.status(HttpStatus.CREATED).body(new OrderResponse(result, "Order placed successfully"));
        else if(result == OrderResults.INSUFFICIENT_STOCK)
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new OrderResponse(result, "Insufficient stock for the requested product"));
        }
        else if(result == OrderResults.PRODUCT_NOT_FOUND)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new OrderResponse(result, "Product not found"));
        }
        else if(result == OrderResults.PAYMENT_FAILED)
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new OrderResponse(result, "Payment failed"));
        }
        else if(result == OrderResults.PAYMENT_PROCESSING_ERROR)
        {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new OrderResponse(result, "Payment processing error"));
        }
        else if(result == OrderResults.UNKNOWN_RESERVATION_ERROR)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OrderResponse(result, "Unknown reservation error"));
        }
        else if(result == OrderResults.UNKNOWN_PAYMENT_ERROR)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OrderResponse(result, "Unknown payment error"));
        }
        else if(result == OrderResults.PAYMENT_SERVICE_UNAVAILABLE)
        {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new OrderResponse(result, "Payment service unavailable"));
        }
        else if(result == OrderResults.INVENTORY_SERVICE_UNAVAILABLE)
        {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new OrderResponse(result, "Inventory service unavailable"));
        }
        else
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OrderResponse(result, "Unknown error"));
        }
    }
}
