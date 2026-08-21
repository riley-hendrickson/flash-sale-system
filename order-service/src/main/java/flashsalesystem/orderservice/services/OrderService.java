package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.enums.OrderResults;
import flashsalesystem.orderservice.enums.PaymentResults;
import flashsalesystem.orderservice.enums.ReservationResults;
import flashsalesystem.orderservice.enums.ReturnResults;
import flashsalesystem.orderservice.exceptions.PaymentProcessorException;
import flashsalesystem.orderservice.exceptions.UnexpectedInventoryException;
import flashsalesystem.orderservice.exceptions.UnexpectedPaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService
{
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderService(InventoryServiceClient inventoryServiceClient, PaymentServiceClient paymentServiceClient)
    {
        this.inventoryServiceClient = inventoryServiceClient;
        this.paymentServiceClient = paymentServiceClient;
    }

    public OrderResults placeOrder(String productId, int quantityRequested, String orderId, double amountDue)
    {
        // reserve stock from inventory service
        ReservationResults reservationResults;
        try
        {
            reservationResults = inventoryServiceClient.reserveStock(productId, quantityRequested);
        }
        catch (UnexpectedInventoryException e)
        {
            return OrderResults.UNKNOWN_RESERVATION_ERROR;
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
                // attempt to return stock to inventory service
                releaseReservation(productId, quantityRequested);
                // return appropriate OrderResult depending on payment error
                return OrderResults.PAYMENT_PROCESSING_ERROR;
            }
            catch(UnexpectedPaymentException e)
            {
                // attempt to return stock to inventory service
                releaseReservation(productId, quantityRequested);
                // return appropriate OrderResult depending on payment error
                return OrderResults.UNKNOWN_PAYMENT_ERROR;
            }
            if(paymentResults == PaymentResults.SUCCESS) return OrderResults.SUCCESS;
            // if payment is unsuccessful, return stock to inventory service
            else
            {
                releaseReservation(productId, quantityRequested);
                // return appropriate OrderResult depending on payment error
                if(paymentResults == PaymentResults.PAYMENT_FAILED) return OrderResults.PAYMENT_FAILED;
                else if(paymentResults == PaymentResults.PAYMENT_SERVICE_UNAVAILABLE) return OrderResults.PAYMENT_SERVICE_UNAVAILABLE;
                else return OrderResults.UNKNOWN_PAYMENT_ERROR;
            }
        }
        // return appropriate OrderResult depending on reservation error
        else if(reservationResults == ReservationResults.INSUFFICIENT_STOCK) return OrderResults.INSUFFICIENT_STOCK;
        else if(reservationResults == ReservationResults.PRODUCT_NOT_FOUND) return OrderResults.PRODUCT_NOT_FOUND;
        else if(reservationResults == ReservationResults.INVENTORY_SERVICE_UNAVAILABLE) return OrderResults.INVENTORY_SERVICE_UNAVAILABLE;
        else return OrderResults.UNKNOWN_RESERVATION_ERROR;
    }


    private void releaseReservation(String productId, int quantityToReturn)
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
