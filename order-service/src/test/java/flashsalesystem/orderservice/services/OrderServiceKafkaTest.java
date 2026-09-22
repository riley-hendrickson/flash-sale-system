package flashsalesystem.orderservice.services;

import flashsalesystem.orderservice.dtos.OrderEvent;
import flashsalesystem.orderservice.enums.OrderResults;
import flashsalesystem.orderservice.enums.PaymentResults;
import flashsalesystem.orderservice.enums.ReservationResults;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Proves OrderEvent actually round-trips through a real Kafka broker with the
 * app's producer config (JacksonJsonSerializer, type headers disabled) - a
 * ArgumentCaptor on a mocked KafkaTemplate (see OrderServiceTest) only proves
 * the call was made, not that the message is actually well-formed on the wire.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(partitions = 1, topics = "order-events")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class OrderServiceKafkaTest
{
    @Autowired
    private OrderService orderService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private InventoryServiceClient inventoryServiceClient;

    @MockitoBean
    private PaymentServiceClient paymentServiceClient;

    private Consumer<String, OrderEvent> consumer;

    @AfterEach
    void closeConsumer()
    {
        if (consumer != null)
        {
            consumer.close();
        }
    }

    @Test
    void placeOrder_publishesOrderEvent_thatDeserializesCorrectly()
    {
        when(inventoryServiceClient.reserveStock(1L, 2)).thenReturn(ReservationResults.SUCCESS);
        when(paymentServiceClient.processPayment("order-1", 19.98)).thenReturn(PaymentResults.SUCCESS);

        consumer = createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "order-events");

        orderService.placeOrder(1L, 2, "order-1", 19.98);

        ConsumerRecord<String, OrderEvent> record = KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10));
        OrderEvent published = record.value();

        assertThat(published.orderId()).isEqualTo("order-1");
        assertThat(published.productId()).isEqualTo(1L);
        assertThat(published.quantity()).isEqualTo(2);
        assertThat(published.amountDue()).isEqualTo(19.98);
        assertThat(published.result()).isEqualTo(OrderResults.SUCCESS);
    }

    private Consumer<String, OrderEvent> createConsumer()
    {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(embeddedKafkaBroker, "order-events-test-group", true);
        consumerProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class);
        consumerProps.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        DefaultKafkaConsumerFactory<String, OrderEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new JacksonJsonDeserializer<>(OrderEvent.class));

        return consumerFactory.createConsumer();
    }
}
