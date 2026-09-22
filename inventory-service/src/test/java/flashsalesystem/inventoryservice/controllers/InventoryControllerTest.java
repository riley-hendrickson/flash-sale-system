package flashsalesystem.inventoryservice.controllers;

import flashsalesystem.inventoryservice.dtos.ProductDTO;
import flashsalesystem.inventoryservice.dtos.ProductListDTO;
import flashsalesystem.inventoryservice.enums.ReservationResults;
import flashsalesystem.inventoryservice.enums.ReturnResults;
import flashsalesystem.inventoryservice.services.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@TestPropertySource(properties = "app.max-inventory=100")
class InventoryControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @TestConfiguration
    static class CacheConfig
    {
        @Bean
        CacheManager cacheManager()
        {
            return new ConcurrentMapCacheManager();
        }
    }

    @Test
    void getProduct_returns200WithBody_whenFound() throws Exception
    {
        when(inventoryService.getProduct(1L)).thenReturn(new ProductDTO(1L, "Widget", BigDecimal.valueOf(9.99), 5));

        mockMvc.perform(get("/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void getProduct_returns404_whenNotFound() throws Exception
    {
        when(inventoryService.getProduct(1L)).thenReturn(null);

        mockMvc.perform(get("/inventory/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllProducts_returns200WithList_whenNonEmpty() throws Exception
    {
        when(inventoryService.getAllProducts()).thenReturn(new ProductListDTO(
                List.of(new ProductDTO(1L, "Widget", BigDecimal.valueOf(9.99), 5))));

        mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Widget"));
    }

    @Test
    void getAllProducts_returns204_whenEmpty() throws Exception
    {
        when(inventoryService.getAllProducts()).thenReturn(new ProductListDTO(List.of()));

        mockMvc.perform(get("/inventory"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reserveStock_returns200_whenSuccess() throws Exception
    {
        when(inventoryService.reserveStock(1L, 2)).thenReturn(ReservationResults.SUCCESS);

        mockMvc.perform(post("/inventory/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityRequested\": 2}"))
                .andExpect(status().isOk());
    }

    @Test
    void reserveStock_returns404_whenProductNotFound() throws Exception
    {
        when(inventoryService.reserveStock(1L, 2)).thenReturn(ReservationResults.PRODUCT_NOT_FOUND);

        mockMvc.perform(post("/inventory/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityRequested\": 2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reserveStock_returns409_whenInsufficientStock() throws Exception
    {
        when(inventoryService.reserveStock(1L, 2)).thenReturn(ReservationResults.INSUFFICIENT_STOCK);

        mockMvc.perform(post("/inventory/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityRequested\": 2}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reserveStock_returns500_whenUnknownError() throws Exception
    {
        when(inventoryService.reserveStock(1L, 2)).thenReturn(ReservationResults.UNKNOWN_ERROR);

        mockMvc.perform(post("/inventory/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityRequested\": 2}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void reserveStock_returns400_whenQuantityRequestedNotPositive() throws Exception
    {
        mockMvc.perform(post("/inventory/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityRequested\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnStock_returns200_whenSuccess() throws Exception
    {
        when(inventoryService.returnStock(1L, 2)).thenReturn(ReturnResults.SUCCESS);

        mockMvc.perform(post("/inventory/1/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityReturned\": 2}"))
                .andExpect(status().isOk());
    }

    @Test
    void returnStock_returns404_whenProductNotFound() throws Exception
    {
        when(inventoryService.returnStock(1L, 2)).thenReturn(ReturnResults.PRODUCT_NOT_FOUND);

        mockMvc.perform(post("/inventory/1/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityReturned\": 2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnStock_returns500_whenUnknownError() throws Exception
    {
        when(inventoryService.returnStock(1L, 2)).thenReturn(ReturnResults.UNKNOWN_ERROR);

        mockMvc.perform(post("/inventory/1/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityReturned\": 2}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returnStock_returns400_whenQuantityReturnedNotPositive() throws Exception
    {
        mockMvc.perform(post("/inventory/1/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityReturned\": 0}"))
                .andExpect(status().isBadRequest());
    }
}
