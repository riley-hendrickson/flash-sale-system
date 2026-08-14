package flashsalesystem.orderservice.controllers;

import flashsalesystem.orderservice.dtos.OrderRequest;
import flashsalesystem.orderservice.enums.ReservationResults;
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
    public ResponseEntity<Void> placeOrder(@RequestBody OrderRequest orderRequest)
    {
        ReservationResults result = orderService.placeOrder(orderRequest.productId(), orderRequest.quantity());

        if(result == ReservationResults.SUCCESS) return ResponseEntity.status(HttpStatus.CREATED).build();
        else if(result == ReservationResults.INSUFFICIENT_STOCK) return ResponseEntity.status(HttpStatus.CONFLICT).build();
        else if(result == ReservationResults.PRODUCT_NOT_FOUND) return ResponseEntity.notFound().build();
        else return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
}
