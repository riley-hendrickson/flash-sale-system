package flashsalesystem.orderservice.metrics;

import flashsalesystem.orderservice.enums.OrderResults;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Component
public class OrderMetrics
{
    private final String METRIC_NAME = "orders.processed";
    private final String OUTCOME_TAG = "outcome";

    private final Map<OrderResults, Counter> countersByOutcome;

    public OrderMetrics(MeterRegistry meterRegistry)
    {
        Map<OrderResults, Counter> counters = new EnumMap<>(OrderResults.class);
        for(OrderResults result : OrderResults.values())
        {
            Counter counter = Counter.builder(METRIC_NAME)
                    .description("Orders processed, by business outcome")
                    .tag(OUTCOME_TAG, result.name())
                    .register(meterRegistry);
            counters.put(result, counter);
        }
        this.countersByOutcome = Collections.unmodifiableMap(counters);
    }

    public void recordOutcome(OrderResults result)
    {
        Objects.requireNonNull(result, "Order result must not be null");
        this.countersByOutcome.get(result).increment();
    }
}
