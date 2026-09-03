package flashsalesystem.inventoryservice.services;

import flashsalesystem.inventoryservice.entities.Product;
import flashsalesystem.inventoryservice.enums.ReservationResults;
import flashsalesystem.inventoryservice.enums.ReturnResults;
import flashsalesystem.inventoryservice.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InventoryService
{
    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository)
    {
        this.productRepository = productRepository;
    }

    @Transactional
    public ReservationResults reserveStock(Long productId, int quantityRequested)
    {
        int result = productRepository.reserveStock(productId, quantityRequested);
        if(result == 0)
        {
            Optional<Product> product = productRepository.findById(productId);
            if(product.isPresent()) return ReservationResults.INSUFFICIENT_STOCK;
            else return ReservationResults.PRODUCT_NOT_FOUND;
        }
        else return ReservationResults.SUCCESS;
    }

    @Transactional
    public ReturnResults returnStock(Long productId, int quantityReturned)
    {
        int result = productRepository.returnStock(productId, quantityReturned);
        if(result == 0) return ReturnResults.PRODUCT_NOT_FOUND;
        else return ReturnResults.SUCCESS;
    }
}
