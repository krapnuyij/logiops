package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class InventoryTransactionIntegrationTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private InventoryRepository inventoryRepository;

  @MockitoBean
  private StockMovementRepository stockMovementRepository;

  @Test
  void rollsBackInventoryWhenMovementPersistenceFails() {
    Product product = productService.create("SKU-ROLLBACK", "상품");
    when(stockMovementRepository.save(any()))
        .thenThrow(new IllegalStateException("이력 저장 실패"));

    assertThatThrownBy(() -> inventoryService.receive(product.getId(), 10))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("이력 저장 실패");

    Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
    assertThat(inventory.getOnHandQuantity()).isZero();
    assertThat(inventory.getReservedQuantity()).isZero();
  }
}
