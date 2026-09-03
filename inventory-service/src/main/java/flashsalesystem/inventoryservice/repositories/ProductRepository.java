package flashsalesystem.inventoryservice.repositories;

import flashsalesystem.inventoryservice.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long>
{
}
