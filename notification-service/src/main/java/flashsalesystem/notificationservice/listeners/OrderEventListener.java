package flashsalesystem.notificationservice.listeners;

import flashsalesystem.notificationservice.events.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener
{
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = "order-events")
    public void orderEvent(OrderEvent orderEvent)
    {
        log.info("Received order event: {}", orderEvent);
    }
}
