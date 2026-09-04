package flashsalesystem.inventoryservice.dtos;

import java.math.BigDecimal;

public record ProductDTO(Long productId, String name, BigDecimal price, int quantity)
{
}
