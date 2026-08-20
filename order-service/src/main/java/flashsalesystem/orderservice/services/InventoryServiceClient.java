package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.dtos.ReservationRequest;
import flashsalesystem.orderservice.dtos.ReturnRequest;
import flashsalesystem.orderservice.enums.ReservationResults;
import flashsalesystem.orderservice.enums.ReturnResults;
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

    public ReservationResults reserveStock(String productId, int quantityRequested)
    {
        return inventoryServiceRestClient.post()
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

    public ReturnResults returnStock(String productId, int quantityToReturn)
    {
        return inventoryServiceRestClient.post()
                .uri("/inventory/{productId}/return", productId)
                .body(new ReturnRequest(quantityToReturn))
                .exchange((request, response) ->
                {
                    if(response.getStatusCode().isSameCodeAs(HttpStatus.OK)) return ReturnResults.SUCCESS;
                    else if(response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) return ReturnResults.PRODUCT_NOT_FOUND;
                    else return ReturnResults.UNKNOWN_ERROR;
                });
    }
}
