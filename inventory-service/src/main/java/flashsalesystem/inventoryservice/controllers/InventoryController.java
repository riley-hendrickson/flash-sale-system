package flashsalesystem.inventoryservice.controllers;

import flashsalesystem.inventoryservice.dtos.ProductDTO;
import flashsalesystem.inventoryservice.dtos.ReturnRequest;
import flashsalesystem.inventoryservice.enums.ReservationResults;
import flashsalesystem.inventoryservice.dtos.ReservationRequest;
import flashsalesystem.inventoryservice.enums.ReturnResults;
import flashsalesystem.inventoryservice.services.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/inventory")
public class InventoryController
{
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService)
    {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long productId)
    {
        Optional<ProductDTO> productDTO = inventoryService.getProduct(productId);
        if(productDTO.isPresent()) return ResponseEntity.ok().body(productDTO.get());
        else return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts()
    {
        List<ProductDTO> productDTOs = inventoryService.getAllProducts();
        if(productDTOs.isEmpty()) return ResponseEntity.noContent().build();
        else return ResponseEntity.ok().body(productDTOs);
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<Void> reserveStock(@PathVariable Long productId, @RequestBody ReservationRequest reservationRequest)
    {
        if(reservationRequest.quantityRequested() <= 0) return ResponseEntity.badRequest().build();

        ReservationResults result = inventoryService.reserveStock(productId, reservationRequest.quantityRequested());

        if(result == ReservationResults.SUCCESS) return ResponseEntity.ok().build();
        else if(result == ReservationResults.PRODUCT_NOT_FOUND) return ResponseEntity.notFound().build();
        else if(result == ReservationResults.INSUFFICIENT_STOCK) return ResponseEntity.status(HttpStatus.CONFLICT).build();
        else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @PostMapping("/{productId}/return")
    public ResponseEntity<Void> returnStock(@PathVariable Long productId, @RequestBody ReturnRequest returnRequest)
    {
        if(returnRequest.quantityReturned() <= 0) return ResponseEntity.badRequest().build();

        ReturnResults result = inventoryService.returnStock(productId, returnRequest.quantityReturned());

        if(result == ReturnResults.SUCCESS) return ResponseEntity.ok().build();
        else if(result == ReturnResults.PRODUCT_NOT_FOUND) return ResponseEntity.notFound().build();
        else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
