package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.dtos.OrderEvent;
import flashsalesystem.orderservice.enums.*;
import flashsalesystem.orderservice.exceptions.PaymentProcessorException;
import flashsalesystem.orderservice.exceptions.UnexpectedInventoryException;
import flashsalesystem.orderservice.exceptions.UnexpectedPaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrderService
{
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderService(InventoryServiceClient inventoryServiceClient, PaymentServiceClient paymentServiceClient, KafkaTemplate<String, OrderEvent> kafkaTemplate)
    {
        this.inventoryServiceClient = inventoryServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public OrderResults placeOrder(Long productId, int quantityRequested, String orderId, double amountDue)
    {
        OrderResults orderResults = OrderResults.SUCCESS;
        // reserve stock from inventory service
        ReservationResults reservationResults;
        try
        {
            reservationResults = inventoryServiceClient.reserveStock(productId, quantityRequested);
        }
        catch (UnexpectedInventoryException e)
        {
            orderResults = OrderResults.UNKNOWN_RESERVATION_ERROR;
            reservationResults = ReservationResults.UNKNOWN_ERROR;
        }
        // if stock reservation is successful, process payment
        if(reservationResults == ReservationResults.SUCCESS)
        {
            PaymentResults paymentResults;
            try
            {
                paymentResults = paymentServiceClient.processPayment(orderId, amountDue);
            }
            catch(PaymentProcessorException e)
            {
                orderResults = OrderResults.PAYMENT_PROCESSING_ERROR;
                paymentResults = PaymentResults.PROCESSOR_ERROR;
            }
            catch(UnexpectedPaymentException e)
            {
                orderResults = OrderResults.UNKNOWN_PAYMENT_ERROR;
                paymentResults = PaymentResults.UNKNOWN_ERROR;
            }
            // if payment is unsuccessful, return stock to inventory service
            if(paymentResults != PaymentResults.SUCCESS)
            {
                releaseReservation(productId, quantityRequested);
                // return appropriate OrderResult depending on payment error
                if(paymentResults == PaymentResults.PAYMENT_FAILED) orderResults = OrderResults.PAYMENT_FAILED;
                else if(paymentResults == PaymentResults.PAYMENT_SERVICE_UNAVAILABLE) orderResults = OrderResults.PAYMENT_SERVICE_UNAVAILABLE;
                else orderResults = OrderResults.UNKNOWN_PAYMENT_ERROR;
            }
        }
        // return appropriate OrderResult depending on reservation error
        else if(reservationResults == ReservationResults.INSUFFICIENT_STOCK) orderResults = OrderResults.INSUFFICIENT_STOCK;
        else if(reservationResults == ReservationResults.PRODUCT_NOT_FOUND) orderResults = OrderResults.PRODUCT_NOT_FOUND;
        else if(reservationResults == ReservationResults.INVENTORY_SERVICE_UNAVAILABLE) orderResults = OrderResults.INVENTORY_SERVICE_UNAVAILABLE;
        else orderResults = OrderResults.UNKNOWN_RESERVATION_ERROR;

        kafkaTemplate.send("order-events", new OrderEvent(orderId, productId, quantityRequested, amountDue, orderResults, Instant.now()));
        return orderResults;
    }


    private void releaseReservation(Long productId, int quantityToReturn)
    {
        ReturnResults returnResults;
        try
        {
            returnResults = inventoryServiceClient.returnStock(productId, quantityToReturn);
        }
        catch (UnexpectedInventoryException e)
        {
            returnResults = ReturnResults.UNKNOWN_ERROR;
        }

        if(returnResults != ReturnResults.SUCCESS)
        {
            log.warn("Failed to return stock reservation for product {}.", productId);
        }
    }
}
