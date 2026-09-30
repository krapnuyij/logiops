package io.github.krapnuyij.logiops.product;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.krapnuyij.logiops.inventory.InventoryRepository;
import io.github.krapnuyij.logiops.inventory.StockMovementRepository;
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
class ProductServiceIntegrationTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @BeforeEach
  void clearProducts() {
    stockMovementRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  @AfterEach
  void cleanUpProducts() {
    stockMovementRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  @Test
  void createsAndFindsProduct() {
    Product created = productService.create(" sku-001 ", " 테스트 상품 ");

    Product found = productService.getById(created.getId());

    assertThat(found.getSku()).isEqualTo("SKU-001");
    assertThat(found.getName()).isEqualTo("테스트 상품");
    assertThat(found.getCreatedAt()).isNotNull();
  }

  @Test
  void rejectsExistingNormalizedSkuBeforeInsert() {
    productService.create("sku-001", "첫 상품");

    assertThatThrownBy(() -> productService.create("SKU-001", "두 번째 상품"))
        .isInstanceOf(DuplicateSkuException.class)
        .extracting("errorCode")
        .isEqualTo("DUPLICATE_SKU");
  }

  @Test
  void throwsWhenProductDoesNotExist() {
    assertThatThrownBy(() -> productService.getById(999L))
        .isInstanceOf(ProductNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo("PRODUCT_NOT_FOUND");
  }

  @Test
  void allowsExactlyOneConcurrentRegistrationForEquivalentSkus() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {
      Future<Boolean> lowercase = executor.submit(registrationTask("sku-001", ready, start));
      Future<Boolean> uppercase = executor.submit(registrationTask("SKU-001", ready, start));

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      List<Boolean> results = List.of(
          lowercase.get(10, TimeUnit.SECONDS),
          uppercase.get(10, TimeUnit.SECONDS)
      );

      assertThat(results).containsExactlyInAnyOrder(true, false);
      assertThat(productRepository.findAll()).singleElement()
          .extracting(Product::getSku)
          .isEqualTo("SKU-001");
    }
    finally {
      start.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  private Callable<Boolean> registrationTask(
      String sku,
      CountDownLatch ready,
      CountDownLatch start
  ) {
    return () -> {
      ready.countDown();
      if (!start.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시 등록 시작 신호를 받지 못했다.");
      }
      try {
        productService.create(sku, "동시 등록 상품");
        return true;
      }
      catch (DuplicateSkuException exception) {
        return false;
      }
    };
  }
}
