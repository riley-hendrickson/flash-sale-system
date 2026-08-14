package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.dtos.ReservationRequest;
import flashsalesystem.orderservice.enums.ReservationResults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrderService
{
    private final RestClient inventoryServiceClient;

    public OrderService(RestClient inventoryServiceClient)
    {
        this.inventoryServiceClient = inventoryServiceClient;
    }

    public ReservationResults placeOrder(String productId, int quantityRequested)
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
}
