package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.enums.ReservationResults;
import flashsalesystem.orderservice.enums.ReturnResults;
import flashsalesystem.orderservice.exceptions.UnexpectedInventoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class InventoryServiceClientTest
{
    private MockRestServiceServer server;
    private InventoryServiceClient client;

    @BeforeEach
    void setUp()
    {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new InventoryServiceClient(builder.build());
    }

    @Test
    void reserveStock_returnsSuccess_on200()
    {
        server.expect(requestTo("http://inventory-service/inventory/1/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        ReservationResults result = client.reserveStock(1L, 2);

        assertThat(result).isEqualTo(ReservationResults.SUCCESS);
        server.verify();
    }

    @Test
    void reserveStock_returnsInsufficientStock_on409()
    {
        server.expect(requestTo("http://inventory-service/inventory/1/reserve"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        ReservationResults result = client.reserveStock(1L, 2);

        assertThat(result).isEqualTo(ReservationResults.INSUFFICIENT_STOCK);
    }

    @Test
    void reserveStock_returnsProductNotFound_on404()
    {
        server.expect(requestTo("http://inventory-service/inventory/1/reserve"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ReservationResults result = client.reserveStock(1L, 2);

        assertThat(result).isEqualTo(ReservationResults.PRODUCT_NOT_FOUND);
    }

    @Test
    void reserveStock_throwsUnexpectedInventoryException_onUnrecognizedStatus()
    {
        server.expect(requestTo("http://inventory-service/inventory/1/reserve"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.reserveStock(1L, 2))
                .isInstanceOf(UnexpectedInventoryException.class);
    }

    @Test
    void returnStock_returnsSuccess_on200()
    {
        server.expect(requestTo("http://inventory-service/inventory/1/return"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        ReturnResults result = client.returnStock(1L, 2);

        assertThat(result).isEqualTo(ReturnResults.SUCCESS);
    }

    @Test
    void returnStock_returnsProductNotFound_on404()
    {
        server.expect(requestTo("http://inventory-service/inventory/1/return"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ReturnResults result = client.returnStock(1L, 2);

        assertThat(result).isEqualTo(ReturnResults.PRODUCT_NOT_FOUND);
    }

    @Test
    void returnStock_throwsUnexpectedInventoryException_onUnrecognizedStatus()
    {
        server.expect(requestTo("http://inventory-service/inventory/1/return"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.returnStock(1L, 2))
                .isInstanceOf(UnexpectedInventoryException.class);
    }

    @Test
    void reserveStockFallback_returnsUnknownError_whenCauseIsUnexpectedInventoryException()
    {
        ReservationResults result = client.reserveStockFallback(1L, 2, new UnexpectedInventoryException("boom"));

        assertThat(result).isEqualTo(ReservationResults.UNKNOWN_ERROR);
    }

    @Test
    void reserveStockFallback_returnsServiceUnavailable_whenCauseIsAnythingElse()
    {
        ReservationResults result = client.reserveStockFallback(1L, 2, new RuntimeException("connection refused"));

        assertThat(result).isEqualTo(ReservationResults.INVENTORY_SERVICE_UNAVAILABLE);
    }

    @Test
    void returnStockFallback_returnsUnknownError_whenCauseIsUnexpectedInventoryException()
    {
        ReturnResults result = client.returnStockFallback(1L, 2, new UnexpectedInventoryException("boom"));

        assertThat(result).isEqualTo(ReturnResults.UNKNOWN_ERROR);
    }

    @Test
    void returnStockFallback_returnsServiceUnavailable_whenCauseIsAnythingElse()
    {
        ReturnResults result = client.returnStockFallback(1L, 2, new RuntimeException("connection refused"));

        assertThat(result).isEqualTo(ReturnResults.INVENTORY_SERVICE_UNAVAILABLE);
    }
}
