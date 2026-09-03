package flashsalesystem.inventoryservice.repositories;

import flashsalesystem.inventoryservice.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>
{
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :quantityRequested WHERE p.id = :id AND p.quantity >= :quantityRequested")
    int reserveStock(@Param("id") Long id, @Param("quantityRequested") int quantityRequested);

    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :quantityReturned WHERE p.id = :id")
    int returnStock(@Param("id") Long id, @Param("quantityReturned") int quantityReturned);
}
