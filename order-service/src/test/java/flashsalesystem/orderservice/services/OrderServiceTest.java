package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.dtos.OrderEvent;
import flashsalesystem.orderservice.enums.OrderResults;
import flashsalesystem.orderservice.enums.PaymentResults;
import flashsalesystem.orderservice.enums.ReservationResults;
import flashsalesystem.orderservice.enums.ReturnResults;
import flashsalesystem.orderservice.exceptions.PaymentProcessorException;
import flashsalesystem.orderservice.exceptions.UnexpectedInventoryException;
import flashsalesystem.orderservice.exceptions.UnexpectedPaymentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest
{
    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_returnsSuccess_whenReservationAndPaymentSucceed()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.SUCCESS);
        when(paymentServiceClient.processPayment("order-1", 19.98)).thenReturn(PaymentResults.SUCCESS);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.SUCCESS);
        verify(inventoryServiceClient, never()).returnStock(1L, 2);
        assertPublishedEvent("order-1", 1L, 2, 19.98, OrderResults.SUCCESS);
    }

    @Test
    void placeOrder_returnsInsufficientStock_andSkipsPayment_whenReservationInsufficient()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.INSUFFICIENT_STOCK);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.INSUFFICIENT_STOCK);
        verify(paymentServiceClient, never()).processPayment(anyString(), anyDouble());
        verify(inventoryServiceClient, never()).returnStock(1L, 2);
        assertPublishedEvent("order-1", 1L, 2, 19.98, OrderResults.INSUFFICIENT_STOCK);
    }

    @Test
    void placeOrder_returnsProductNotFound_whenReservationProductNotFound()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.PRODUCT_NOT_FOUND);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.PRODUCT_NOT_FOUND);
        verify(paymentServiceClient, never()).processPayment(anyString(), anyDouble());
    }

    @Test
    void placeOrder_returnsInventoryServiceUnavailable_whenReservationUnavailable()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.INVENTORY_SERVICE_UNAVAILABLE);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.INVENTORY_SERVICE_UNAVAILABLE);
        verify(paymentServiceClient, never()).processPayment(anyString(), anyDouble());
    }

    @Test
    void placeOrder_returnsUnknownReservationError_whenReservationThrowsUnexpectedException()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenThrow(new UnexpectedInventoryException("boom"));

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.UNKNOWN_RESERVATION_ERROR);
        verify(paymentServiceClient, never()).processPayment(anyString(), anyDouble());
    }

    @Test
    void placeOrder_returnsPaymentFailed_andReleasesReservation_whenPaymentFails()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.SUCCESS);
        when(paymentServiceClient.processPayment("order-1", 19.98)).thenReturn(PaymentResults.PAYMENT_FAILED);
        when(inventoryServiceClient.returnStock(1L, 2)).thenReturn(ReturnResults.SUCCESS);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.PAYMENT_FAILED);
        verify(inventoryServiceClient).returnStock(1L, 2);
    }

    @Test
    void placeOrder_returnsPaymentProcessingError_andReleasesReservation_whenPaymentThrowsProcessorException()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.SUCCESS);
        when(paymentServiceClient.processPayment("order-1", 19.98)).thenThrow(new PaymentProcessorException("down"));
        when(inventoryServiceClient.returnStock(1L, 2)).thenReturn(ReturnResults.SUCCESS);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.PAYMENT_PROCESSING_ERROR);
        verify(inventoryServiceClient).returnStock(1L, 2);
    }

    @Test
    void placeOrder_returnsUnknownPaymentError_andReleasesReservation_whenPaymentThrowsUnexpectedException()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.SUCCESS);
        when(paymentServiceClient.processPayment("order-1", 19.98)).thenThrow(new UnexpectedPaymentException("boom"));
        when(inventoryServiceClient.returnStock(1L, 2)).thenReturn(ReturnResults.SUCCESS);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.UNKNOWN_PAYMENT_ERROR);
        verify(inventoryServiceClient).returnStock(1L, 2);
    }

    @Test
    void placeOrder_returnsPaymentServiceUnavailable_andReleasesReservation_whenPaymentUnavailable()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.SUCCESS);
        when(paymentServiceClient.processPayment("order-1", 19.98)).thenReturn(PaymentResults.PAYMENT_SERVICE_UNAVAILABLE);
        when(inventoryServiceClient.returnStock(1L, 2)).thenReturn(ReturnResults.SUCCESS);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.PAYMENT_SERVICE_UNAVAILABLE);
        verify(inventoryServiceClient).returnStock(1L, 2);
    }

    @Test
    void placeOrder_stillReturnsPaymentFailed_whenReleasingReservationAlsoFails()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.SUCCESS);
        when(paymentServiceClient.processPayment("order-1", 19.98)).thenReturn(PaymentResults.PAYMENT_FAILED);
        when(inventoryServiceClient.returnStock(1L, 2)).thenReturn(ReturnResults.PRODUCT_NOT_FOUND);

        OrderResults result = orderService.placeOrder(1L, 2, "order-1", 19.98);

        assertThat(result).isEqualTo(OrderResults.PAYMENT_FAILED);
    }

    private void assertPublishedEvent(String orderId, Long productId, int quantity, double amountDue, OrderResults expectedResult)
    {
        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(eq("order-events"), eventCaptor.capture());
        OrderEvent published = eventCaptor.getValue();

        assertThat(published.orderId()).isEqualTo(orderId);
        assertThat(published.productId()).isEqualTo(productId);
        assertThat(published.quantity()).isEqualTo(quantity);
        assertThat(published.amountDue()).isEqualTo(amountDue);
        assertThat(published.result()).isEqualTo(expectedResult);
    }
}
