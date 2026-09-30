package io.github.krapnuyij.logiops.inventory;

import java.time.Instant;

import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class InventoryRepositoryTest {

  private static final Instant CREATED_AT = Instant.parse("2026-09-30T01:00:00Z");

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void persistsLoadsAndLocksInventory() {
    Product product = saveProduct("SKU-001");
    Inventory saved = inventoryRepository.saveAndFlush(Inventory.initialize(product, CREATED_AT));
    entityManager.clear();

    Inventory found = inventoryRepository.findByProductIdForUpdate(product.getId()).orElseThrow();

    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getProduct().getId()).isEqualTo(product.getId());
    assertThat(found.getAvailableQuantity()).isZero();
  }

  @Test
  void enforcesOneInventoryPerProduct() {
    Product product = saveProduct("SKU-001");
    inventoryRepository.saveAndFlush(Inventory.initialize(product, CREATED_AT));

    assertThatThrownBy(() -> jdbcTemplate.update(
        "insert into inventories "
            + "(product_id, on_hand_quantity, reserved_quantity, updated_at) values (?, 0, 0, ?)",
        product.getId(),
        CREATED_AT
    )).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsNegativeOnHandQuantityAtDatabaseBoundary() {
    Product product = saveProduct("SKU-001");

    assertThatThrownBy(() -> jdbcTemplate.update(
        "insert into inventories "
            + "(product_id, on_hand_quantity, reserved_quantity, updated_at) values (?, -1, 0, ?)",
        product.getId(),
        CREATED_AT
    )).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsReservedQuantityGreaterThanOnHandAtDatabaseBoundary() {
    Product product = saveProduct("SKU-001");

    assertThatThrownBy(() -> jdbcTemplate.update(
        "insert into inventories "
            + "(product_id, on_hand_quantity, reserved_quantity, updated_at) values (?, 1, 2, ?)",
        product.getId(),
        CREATED_AT
    )).isInstanceOf(DataIntegrityViolationException.class);
  }

  private Product saveProduct(String sku) {
    return productRepository.saveAndFlush(Product.create(sku, "상품", CREATED_AT));
  }
}
