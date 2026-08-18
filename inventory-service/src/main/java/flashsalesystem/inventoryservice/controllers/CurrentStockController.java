package flashsalesystem.inventoryservice.controllers;

import flashsalesystem.inventoryservice.dtos.ReturnRequest;
import flashsalesystem.inventoryservice.enums.ReservationResults;
import flashsalesystem.inventoryservice.dtos.ReservationRequest;
import flashsalesystem.inventoryservice.enums.ReturnResults;
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
    public ResponseEntity<Void> reserveStock(@PathVariable String productId, @RequestBody ReservationRequest reservationRequest)
    {
        if(reservationRequest.quantityRequested() <= 0) return ResponseEntity.badRequest().build();

        ReservationResults result = inventoryService.reserve(productId, reservationRequest.quantityRequested());

        if(result == ReservationResults.SUCCESS) return ResponseEntity.ok().build();
        else if(result == ReservationResults.PRODUCT_NOT_FOUND) return ResponseEntity.notFound().build();
        else if(result == ReservationResults.INSUFFICIENT_STOCK) return ResponseEntity.status(HttpStatus.CONFLICT).build();
        else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @PostMapping("/{productId}/return")
    public ResponseEntity<Void> returnStock(@PathVariable String productId, @RequestBody ReturnRequest returnRequest)
    {
        if(returnRequest.quantityReturned() <= 0) return ResponseEntity.badRequest().build();

        ReturnResults result = inventoryService.returnStock(productId, returnRequest.quantityReturned());

        if(result == ReturnResults.SUCCESS) return ResponseEntity.ok().build();
        else if(result == ReturnResults.PRODUCT_NOT_FOUND) return ResponseEntity.notFound().build();
        else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
