package io.github.krapnuyij.logiops.inventory;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  Optional<Inventory> findByProductId(long productId);

  boolean existsByProductId(long productId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select inventory from Inventory inventory "
      + "join fetch inventory.product where inventory.product.id = :productId")
  Optional<Inventory> findByProductIdForUpdate(@Param("productId") long productId);
}
