package flashsalesystem.orderservice.controllers;

import flashsalesystem.orderservice.config.SecurityConfig;
import flashsalesystem.orderservice.enums.OrderResults;
import flashsalesystem.orderservice.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest
{
    private static final String ORDER_REQUEST_JSON =
            "{\"productId\": 1, \"quantity\": 2, \"orderId\": \"order-1\", \"amountDue\": 19.98}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    // Satisfies the SecurityFilterChain's dependency on a JwtDecoder bean without
    // ever contacting a real Keycloak instance; jwt() below bypasses it entirely.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void placeOrder_returns401_whenUnauthenticated() throws Exception
    {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeOrder_returns201_whenSuccess() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.SUCCESS);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void placeOrder_returns409_whenInsufficientStock() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.INSUFFICIENT_STOCK);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void placeOrder_returns404_whenProductNotFound() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.PRODUCT_NOT_FOUND);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void placeOrder_returns409_whenPaymentFailed() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.PAYMENT_FAILED);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void placeOrder_returns502_whenPaymentProcessingError() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.PAYMENT_PROCESSING_ERROR);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isBadGateway());
    }

    @Test
    void placeOrder_returns503_whenPaymentServiceUnavailable() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.PAYMENT_SERVICE_UNAVAILABLE);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void placeOrder_returns503_whenInventoryServiceUnavailable() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.INVENTORY_SERVICE_UNAVAILABLE);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void placeOrder_returns500_whenUnknownReservationError() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.UNKNOWN_RESERVATION_ERROR);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void placeOrder_returns500_whenUnknownPaymentError() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.UNKNOWN_PAYMENT_ERROR);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void placeOrder_returns500_whenUnknownError() throws Exception
    {
        when(orderService.placeOrder(1L, 2, "order-1", 19.98)).thenReturn(OrderResults.UNKNOWN_ERROR);

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_REQUEST_JSON))
                .andExpect(status().isInternalServerError());
    }
}
