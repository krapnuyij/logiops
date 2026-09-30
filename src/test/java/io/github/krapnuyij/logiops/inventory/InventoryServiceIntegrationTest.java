package io.github.krapnuyij.logiops.inventory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceIntegrationTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @Autowired
  private InventoryService inventoryService;

  @BeforeEach
  void clearData() {
    stockMovementRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  @AfterEach
  void cleanUpData() {
    stockMovementRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  @Test
  void receivesInventoryAndRecordsMovementInOneTransaction() {
    Product product = productService.create("SKU-001", "상품");

    Inventory received = inventoryService.receive(product.getId(), 10);

    assertThat(received.getOnHandQuantity()).isEqualTo(10);
    assertThat(received.getReservedQuantity()).isZero();
    assertThat(received.getAvailableQuantity()).isEqualTo(10);
    assertThat(stockMovementRepository.findAll()).singleElement().satisfies(movement -> {
      assertThat(movement.getProduct().getId()).isEqualTo(product.getId());
      assertThat(movement.getType()).isEqualTo(StockMovementType.RECEIPT);
      assertThat(movement.getOnHandDelta()).isEqualTo(10);
      assertThat(movement.getReservedDelta()).isZero();
      assertThat(movement.getOnHandAfter()).isEqualTo(10);
      assertThat(movement.getReservedAfter()).isZero();
      assertThat(movement.getOccurredAt()).isEqualTo(received.getUpdatedAt());
    });
  }

  @Test
  void distinguishesMissingProductFromMissingInventory() {
    assertThatThrownBy(() -> inventoryService.getByProductId(999L))
        .isInstanceOf(io.github.krapnuyij.logiops.product.ProductNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo("PRODUCT_NOT_FOUND");

    Product productWithoutInventory = productRepository.saveAndFlush(
        Product.create("SKU-ORPHAN", "상품", java.time.Instant.parse("2026-09-30T01:00:00Z"))
    );

    assertThatThrownBy(() -> inventoryService.getByProductId(productWithoutInventory.getId()))
        .isInstanceOf(InventoryNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo("INVENTORY_NOT_FOUND");
  }

  @Test
  void serializesConcurrentReceiptsForSameProduct() throws Exception {
    Product product = productService.create("SKU-001", "상품");
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {
      Future<Long> first = executor.submit(receiptTask(product.getId(), 10, ready, start));
      Future<Long> second = executor.submit(receiptTask(product.getId(), 20, ready, start));

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<Long> results = List.of(
          first.get(10, TimeUnit.SECONDS),
          second.get(10, TimeUnit.SECONDS)
      );
      assertThat(results).contains(30L).doesNotHaveDuplicates();
      assertThat(results).anyMatch(value -> value == 10L || value == 20L);

      Inventory inventory = inventoryService.getByProductId(product.getId());
      assertThat(inventory.getOnHandQuantity()).isEqualTo(30);
      assertThat(stockMovementRepository.count()).isEqualTo(2);
    }
    finally {
      start.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  private Callable<Long> receiptTask(
      long productId,
      long quantity,
      CountDownLatch ready,
      CountDownLatch start
  ) {
    return () -> {
      ready.countDown();
      if (!start.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시 입고 시작 신호를 받지 못했다.");
      }
      return inventoryService.receive(productId, quantity).getOnHandQuantity();
    };
  }
}
