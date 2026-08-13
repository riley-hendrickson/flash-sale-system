package flashsalesystem.inventoryservice.models;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class Inventory
{
    private final ConcurrentHashMap<String, Integer> stock = new ConcurrentHashMap<>();

    public boolean reserve(String productId, int quantityRequested)
    {
        AtomicBoolean success = new AtomicBoolean(false);

        stock.compute(productId, (id, currentQuantity) ->
        {
            if(currentQuantity == null || currentQuantity < quantityRequested)
            {
                success.set(false);
                return currentQuantity;
            }
            else
            {
                success.set(true);
                return currentQuantity - quantityRequested;
            }
        });

        return success.get();
    }

    public Inventory()
    {
        stock.put("1", 1);
        stock.put("2", 10);
        stock.put("3", 10);
    }
}
