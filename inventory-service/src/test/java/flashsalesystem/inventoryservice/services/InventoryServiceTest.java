package flashsalesystem.inventoryservice.services;

import flashsalesystem.inventoryservice.dtos.ProductDTO;
import flashsalesystem.inventoryservice.dtos.ProductListDTO;
import flashsalesystem.inventoryservice.entities.Product;
import flashsalesystem.inventoryservice.enums.ReservationResults;
import flashsalesystem.inventoryservice.enums.ReturnResults;
import flashsalesystem.inventoryservice.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest
{
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void getProduct_returnsMappedDto_whenProductExists()
    {
        Product product = new Product(1L, "Widget", BigDecimal.valueOf(9.99), 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDTO result = inventoryService.getProduct(1L);

        assertThat(result).isEqualTo(new ProductDTO(1L, "Widget", BigDecimal.valueOf(9.99), 5));
    }

    @Test
    void getProduct_returnsNull_whenProductDoesNotExist()
    {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        ProductDTO result = inventoryService.getProduct(1L);

        assertThat(result).isNull();
    }

    @Test
    void getAllProducts_returnsMappedList()
    {
        Product first = new Product(1L, "Widget", BigDecimal.valueOf(9.99), 5);
        Product second = new Product(2L, "Gadget", BigDecimal.valueOf(19.99), 10);
        when(productRepository.findAll()).thenReturn(List.of(first, second));

        ProductListDTO result = inventoryService.getAllProducts();

        assertThat(result.products()).containsExactly(
                new ProductDTO(1L, "Widget", BigDecimal.valueOf(9.99), 5),
                new ProductDTO(2L, "Gadget", BigDecimal.valueOf(19.99), 10));
    }

    @Test
    void getAllProducts_returnsEmptyList_whenNoProductsExist()
    {
        when(productRepository.findAll()).thenReturn(List.of());

        ProductListDTO result = inventoryService.getAllProducts();

        assertThat(result.products()).isEmpty();
    }

    @Test
    void reserveStock_returnsSuccess_whenRowUpdated()
    {
        when(productRepository.reserveStock(1L, 2)).thenReturn(1);

        ReservationResults result = inventoryService.reserveStock(1L, 2);

        assertThat(result).isEqualTo(ReservationResults.SUCCESS);
        verify(productRepository, never()).findById(any());
    }

    @Test
    void reserveStock_returnsInsufficientStock_whenNoRowUpdatedButProductExists()
    {
        when(productRepository.reserveStock(1L, 100)).thenReturn(0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product(1L, "Widget", BigDecimal.valueOf(9.99), 5)));

        ReservationResults result = inventoryService.reserveStock(1L, 100);

        assertThat(result).isEqualTo(ReservationResults.INSUFFICIENT_STOCK);
    }

    @Test
    void reserveStock_returnsProductNotFound_whenNoRowUpdatedAndProductMissing()
    {
        when(productRepository.reserveStock(1L, 1)).thenReturn(0);
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        ReservationResults result = inventoryService.reserveStock(1L, 1);

        assertThat(result).isEqualTo(ReservationResults.PRODUCT_NOT_FOUND);
    }

    @Test
    void returnStock_returnsSuccess_whenRowUpdated()
    {
        when(productRepository.returnStock(eq(1L), eq(3))).thenReturn(1);

        ReturnResults result = inventoryService.returnStock(1L, 3);

        assertThat(result).isEqualTo(ReturnResults.SUCCESS);
    }

    @Test
    void returnStock_returnsProductNotFound_whenNoRowUpdated()
    {
        when(productRepository.returnStock(eq(1L), eq(3))).thenReturn(0);

        ReturnResults result = inventoryService.returnStock(1L, 3);

        assertThat(result).isEqualTo(ReturnResults.PRODUCT_NOT_FOUND);
    }
}
