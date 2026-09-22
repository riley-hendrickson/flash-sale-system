package flashsalesystem.inventoryservice.repositories;

import flashsalesystem.inventoryservice.entities.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "app.max-inventory=100")
class ProductRepositoryIT
{
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:14-alpine");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long productId;

    @TestConfiguration
    static class CacheConfig
    {
        @Bean
        CacheManager cacheManager()
        {
            return new ConcurrentMapCacheManager();
        }
    }

    @BeforeEach
    void seedProduct()
    {
        Product seeded = productRepository.save(
                new Product(null, "Repo Test Product", BigDecimal.valueOf(19.99), 5));
        productId = seeded.getId();
    }

    @Test
    void reserveStock_succeeds_whenQuantityExactlyMatchesAvailableStock()
    {
        int rowsUpdated = productRepository.reserveStock(productId, 5);
        entityManager.clear();

        assertThat(rowsUpdated).isEqualTo(1);
        assertThat(productRepository.findById(productId).orElseThrow().getQuantity()).isZero();
    }

    @Test
    void reserveStock_fails_whenQuantityExceedsAvailableStock()
    {
        int rowsUpdated = productRepository.reserveStock(productId, 6);
        entityManager.clear();

        assertThat(rowsUpdated).isZero();
        assertThat(productRepository.findById(productId).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    void reserveStock_affectsNoRows_whenProductDoesNotExist()
    {
        int rowsUpdated = productRepository.reserveStock(-1L, 1);

        assertThat(rowsUpdated).isZero();
    }

    @Test
    void returnStock_incrementsQuantity_whenProductExists()
    {
        int rowsUpdated = productRepository.returnStock(productId, 3);
        entityManager.clear();

        assertThat(rowsUpdated).isEqualTo(1);
        assertThat(productRepository.findById(productId).orElseThrow().getQuantity()).isEqualTo(8);
    }

    @Test
    void returnStock_affectsNoRows_whenProductDoesNotExist()
    {
        int rowsUpdated = productRepository.returnStock(-1L, 1);

        assertThat(rowsUpdated).isZero();
    }
}
