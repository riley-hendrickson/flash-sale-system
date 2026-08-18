package flashsalesystem.inventoryservice.services;

import flashsalesystem.inventoryservice.enums.ReservationResults;
import flashsalesystem.inventoryservice.enums.ReturnResults;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class InventoryService
{
    private final ConcurrentHashMap<String, Integer> stock = new ConcurrentHashMap<>();

    public ReservationResults reserve(String productId, int quantityRequested)
    {
        AtomicReference<ReservationResults> result = new AtomicReference<>();

        stock.compute(productId, (id, currentQuantity) ->
        {
            // if the product does not exist, return current quantity and set result accordingly
            if(currentQuantity == null)
            {
                result.set(ReservationResults.PRODUCT_NOT_FOUND);
                return currentQuantity;
            }
            // if the product is not sufficiently stocked, return current quantity and set result accordingly
            else if(currentQuantity < quantityRequested)
            {
                result.set(ReservationResults.INSUFFICIENT_STOCK);
                return currentQuantity;
            }
            // if the product has adequate stock, set result accordingly and return the remaining quantity
            else
            {
                result.set(ReservationResults.SUCCESS);
                return currentQuantity - quantityRequested;
            }
        });

        return result.get();
    }

    public ReturnResults returnStock(String productId, int quantityReturned)
    {
        AtomicReference<ReturnResults> result = new AtomicReference<>();

        stock.compute(productId, (id, currentQuantity) ->
        {
            // if the product does not exist, return current quantity and set result accordingly
            if(currentQuantity == null)
            {
                result.set(ReturnResults.PRODUCT_NOT_FOUND);
                return currentQuantity;
            }
            else
            {
                result.set(ReturnResults.SUCCESS);
                return currentQuantity + quantityReturned;
            }
        });

        return result.get();
    }

    public InventoryService()
    {
        stock.put("1", 1);
        stock.put("2", 10);
        stock.put("3", 10);
    }
}
