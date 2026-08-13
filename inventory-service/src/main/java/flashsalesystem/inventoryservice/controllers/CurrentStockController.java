package flashsalesystem.inventoryservice.controllers;

import flashsalesystem.inventoryservice.enums.ReservationResults;
import flashsalesystem.inventoryservice.dtos.ReservationRequest;
import flashsalesystem.inventoryservice.services.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/inventory")
public class CurrentStockController
{
    private final InventoryService inventoryService;

    public CurrentStockController(InventoryService inventoryService)
    {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<Void> reserve(@PathVariable String productId, @RequestBody ReservationRequest reservationRequest)
    {
        if(reservationRequest.quantityRequested() <= 0) return ResponseEntity.badRequest().build();

        ReservationResults result = inventoryService.reserve(productId, reservationRequest.quantityRequested());

        if(result == ReservationResults.SUCCESS) return ResponseEntity.ok().build();
        else if(result == ReservationResults.PRODUCT_NOT_FOUND) return ResponseEntity.notFound().build();
        else return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
