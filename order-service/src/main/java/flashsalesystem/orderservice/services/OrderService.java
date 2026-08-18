package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.dtos.PaymentRequest;
import flashsalesystem.orderservice.dtos.ReservationRequest;
import flashsalesystem.orderservice.dtos.ReturnRequest;
import flashsalesystem.orderservice.enums.OrderResults;
import flashsalesystem.orderservice.enums.PaymentResults;
import flashsalesystem.orderservice.enums.ReservationResults;
import flashsalesystem.orderservice.enums.ReturnResults;
import flashsalesystem.orderservice.exceptions.PaymentProcessorException;
import flashsalesystem.orderservice.exceptions.UnexpectedPaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrderService
{
    private final RestClient inventoryServiceClient;
    private final RestClient paymentServiceClient;

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderService(RestClient inventoryServiceClient, RestClient paymentServiceClient)
    {
        this.inventoryServiceClient = inventoryServiceClient;
        this.paymentServiceClient = paymentServiceClient;
    }

    public OrderResults placeOrder(String productId, int quantityRequested, String orderId, double amountDue)
    {
        // reserve stock from inventory service
        ReservationResults reservationResults = reserveStock(productId, quantityRequested);
        // if stock reservation is successful, process payment
        if(reservationResults == ReservationResults.SUCCESS)
        {
            PaymentResults paymentResults;
            try
            {
                paymentResults = processPayment(orderId, amountDue);
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
                else return OrderResults.UNKNOWN_PAYMENT_ERROR;
            }
        }
        // return appropriate OrderResult depending on reservation error
        else if(reservationResults == ReservationResults.INSUFFICIENT_STOCK) return OrderResults.INSUFFICIENT_STOCK;
        else if(reservationResults == ReservationResults.PRODUCT_NOT_FOUND) return OrderResults.PRODUCT_NOT_FOUND;
        else return OrderResults.UNKNOWN_RESERVATION_ERROR;
    }

    private ReservationResults reserveStock(String productId, int quantityRequested)
    {
        return inventoryServiceClient.post()
                .uri("/inventory/{productId}/reserve", productId)
                .body(new ReservationRequest(quantityRequested))
                .exchange((request, response) ->
                {
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.OK)) return ReservationResults.SUCCESS;
                    else if (response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) return ReservationResults.INSUFFICIENT_STOCK;
                    else if(response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) return ReservationResults.PRODUCT_NOT_FOUND;
                    else return ReservationResults.UNKNOWN_ERROR;
                });
    }

    private ReturnResults returnStock(String productId, int quantityToReturn)
    {
        return inventoryServiceClient.post()
                .uri("/inventory/{productId}/return", productId)
                .body(new ReturnRequest(quantityToReturn))
                .exchange((request, response) ->
                {
                    if(response.getStatusCode().isSameCodeAs(HttpStatus.OK)) return ReturnResults.SUCCESS;
                    else if(response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) return ReturnResults.PRODUCT_NOT_FOUND;
                    else return ReturnResults.UNKNOWN_ERROR;
                });
    }

    private void releaseReservation(String productId, int quantityToReturn)
    {
        if(returnStock(productId, quantityToReturn) != ReturnResults.SUCCESS)
        {
            log.warn("Failed to return stock reservation for product {}.", productId);
        }
    }

    private PaymentResults processPayment(String orderId, double amountDue)
    {
        return paymentServiceClient.post()
                .uri("/payments")
                .body(new PaymentRequest(orderId, amountDue))
                .exchange((request, response) ->
                {
                    if(response.getStatusCode().isSameCodeAs(HttpStatus.OK)) return PaymentResults.SUCCESS;
                    else if(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) return PaymentResults.PAYMENT_FAILED;
                    else if(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_GATEWAY))
                    {
                        throw new PaymentProcessorException("Payment processor error");
                    }
                    else
                    {
                        throw new UnexpectedPaymentException("Unexpected payment error");
                    }
                });
    }
}
