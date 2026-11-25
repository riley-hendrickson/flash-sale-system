package flashsalesystem.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class OrderServiceApplication
{
    private static final Logger log = LoggerFactory.getLogger(OrderServiceApplication.class);

    public static void main(String[] args)
    {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @Bean
    public RestClient inventoryServiceRestClient(RestClient.Builder builder, @Value("${services.inventory-service.url}") String inventoryServiceUrl)
    {
        return builder.baseUrl(inventoryServiceUrl).build();
    }

    @Bean
    public RestClient paymentServiceRestClient(RestClient.Builder builder, @Value("${services.payment-service.url}") String paymentServiceUrl)
    {
        return builder.baseUrl(paymentServiceUrl).build();
    }
}
