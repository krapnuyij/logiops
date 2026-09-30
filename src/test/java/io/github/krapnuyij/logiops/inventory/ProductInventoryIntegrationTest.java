package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductInventoryIntegrationTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @BeforeEach
  void clearData() {
    stockMovementRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  @Test
  void createsZeroInventoryInSameProductCreationFlow() {
    Product product = productService.create("SKU-001", "상품");

    Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();

    assertThat(inventory.getOnHandQuantity()).isZero();
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(inventory.getAvailableQuantity()).isZero();
    assertThat(inventory.getUpdatedAt()).isEqualTo(product.getCreatedAt());
  }
}
