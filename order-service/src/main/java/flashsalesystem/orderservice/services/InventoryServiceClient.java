package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.dtos.ReservationRequest;
import flashsalesystem.orderservice.dtos.ReturnRequest;
import flashsalesystem.orderservice.enums.ReservationResults;
import flashsalesystem.orderservice.enums.ReturnResults;
import flashsalesystem.orderservice.exceptions.UnexpectedInventoryException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class InventoryServiceClient
{
    private final RestClient inventoryServiceRestClient;

    public InventoryServiceClient(RestClient inventoryServiceRestClient)
    {
        this.inventoryServiceRestClient = inventoryServiceRestClient;
    }

    @Retry(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    @CircuitBreaker(name = "inventoryService")
    public ReservationResults reserveStock(String productId, int quantityRequested)
    {
        return inventoryServiceRestClient.post()
                .uri("/inventory/{productId}/reserve", productId)
                .body(new ReservationRequest(quantityRequested))
                .exchange((request, response) ->
                {
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.OK)) return ReservationResults.SUCCESS;
                    else if (response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT))
                        return ReservationResults.INSUFFICIENT_STOCK;
                    else if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND))
                        return ReservationResults.PRODUCT_NOT_FOUND;
                    else throw new UnexpectedInventoryException("Unexpected inventory error during stock reservation");
                });
    }

    @Retry(name = "inventoryService", fallbackMethod = "returnStockFallback")
    @CircuitBreaker(name = "inventoryService")
    public ReturnResults returnStock(String productId, int quantityToReturn)
    {
        return inventoryServiceRestClient.post()
                .uri("/inventory/{productId}/return", productId)
                .body(new ReturnRequest(quantityToReturn))
                .exchange((request, response) ->
                {
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.OK)) return ReturnResults.SUCCESS;
                    else if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND))
                        return ReturnResults.PRODUCT_NOT_FOUND;
                    else throw new UnexpectedInventoryException("Unexpected inventory error during stock return");
                });
    }

    public ReservationResults reserveStockFallback(String productId, int quantityRequested, Throwable exception)
    {
        if(exception instanceof UnexpectedInventoryException) return ReservationResults.UNKNOWN_ERROR;
        else return ReservationResults.INVENTORY_SERVICE_UNAVAILABLE;
    }

    public ReturnResults returnStockFallback(String productId, int quantityToReturn, Throwable exception)
    {
        if(exception instanceof UnexpectedInventoryException) return ReturnResults.UNKNOWN_ERROR;
        else return ReturnResults.INVENTORY_SERVICE_UNAVAILABLE;
    }
}
