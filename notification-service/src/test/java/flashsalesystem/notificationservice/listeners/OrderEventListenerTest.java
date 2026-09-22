package flashsalesystem.notificationservice.listeners;

import flashsalesystem.notificationservice.enums.OrderResults;
import flashsalesystem.notificationservice.events.OrderEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * order-service and notification-service each maintain their own copy of the
 * OrderEvent shape (no shared library) and disable Kafka's type-info headers,
 * so the only thing holding this contract together is the consumer's
 * configured "spring.json.value.default.type" matching the producer's JSON
 * shape field-for-field. This proves that contract actually holds, not just
 * that the listener method itself is correct.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(partitions = 1, topics = "order-events")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class OrderEventListenerTest
{
    @MockitoSpyBean
    private OrderEventListener orderEventListener;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void listener_consumesAndDeserializes_orderEventFromTheWire()
    {
        KafkaTemplate<String, OrderEvent> producer = createProducer();
        OrderEvent event = new OrderEvent("order-1", 1L, 2, 19.98, OrderResults.SUCCESS, Instant.now());

        producer.send("order-events", event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(orderEventListener).orderEvent(argThat(received ->
                        received.orderId().equals("order-1")
                                && received.productId().equals(1L)
                                && received.quantity() == 2
                                && received.amountDue() == 19.98
                                && received.result() == OrderResults.SUCCESS)));
    }

    private KafkaTemplate<String, OrderEvent> createProducer()
    {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        // matches order-service's real producer config: no type headers, so
        // this only works if the consumer's default-type + trusted-packages
        // config is actually correct.
        producerProps.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        DefaultKafkaProducerFactory<String, OrderEvent> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new JacksonJsonSerializer<>());

        return new KafkaTemplate<>(producerFactory);
    }
}
