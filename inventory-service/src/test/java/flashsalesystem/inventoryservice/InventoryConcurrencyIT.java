package flashsalesystem.inventoryservice;

import flashsalesystem.inventoryservice.entities.Product;
import flashsalesystem.inventoryservice.enums.ReservationResults;
import flashsalesystem.inventoryservice.repositories.ProductRepository;
import flashsalesystem.inventoryservice.services.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "app.max-inventory=100",
        "spring.datasource.hikari.maximum-pool-size=50"
})
class InventoryConcurrencyIT
{
    private static final int THREAD_COUNT = 50;
    private static final int SEEDED_QUANTITY = 20;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:14-alpine");

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    private Long seededProductId;

    @BeforeEach
    void seedProduct()
    {
        Product seeded = productRepository.save(
                new Product(null, "Concurrency Test Product", BigDecimal.valueOf(9.99), SEEDED_QUANTITY));
        seededProductId = seeded.getId();
    }

    @AfterEach
    void cleanUp()
    {
        productRepository.deleteById(seededProductId);
    }

    @Test
    void concurrentReservations_exactlySeededQuantitySucceed() throws InterruptedException
    {
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        List<ReservationResults> results = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        for(int i = 0; i < THREAD_COUNT; i++)
        {
            executor.submit(() ->
            {
                readyLatch.countDown();
                try
                {
                    startLatch.await();
                    results.add(inventoryService.reserveStock(seededProductId, 1));
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean completedInTime = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).isTrue();
        assertThat(results).hasSize(THREAD_COUNT);

        long successCount = results.stream().filter(r -> r == ReservationResults.SUCCESS).count();
        long insufficientStockCount = results.stream().filter(r -> r == ReservationResults.INSUFFICIENT_STOCK).count();

        assertThat(successCount).isEqualTo(SEEDED_QUANTITY);
        assertThat(insufficientStockCount).isEqualTo(THREAD_COUNT - SEEDED_QUANTITY);

        int remainingQuantity = productRepository.findById(seededProductId)
                .orElseThrow()
                .getQuantity();
        assertThat(remainingQuantity).isEqualTo(0);
    }
}
