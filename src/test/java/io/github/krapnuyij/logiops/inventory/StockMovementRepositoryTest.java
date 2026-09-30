package io.github.krapnuyij.logiops.inventory;

import java.time.Instant;

import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StockMovementRepositoryTest {

  private static final Instant CREATED_AT = Instant.parse("2026-09-30T01:00:00Z");
  private static final Sort NEWEST_FIRST = Sort.by(
      Sort.Order.desc("occurredAt"),
      Sort.Order.desc("id")
  );

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @Test
  void filtersByProductAndTypeAndSortsNewestFirst() {
    Product firstProduct = saveProduct("SKU-001");
    Product secondProduct = saveProduct("SKU-002");
    StockMovement older = saveReceipt(firstProduct, 3, CREATED_AT.plusSeconds(10));
    StockMovement newer = saveReceipt(firstProduct, 2, CREATED_AT.plusSeconds(20));
    saveReceipt(secondProduct, 5, CREATED_AT.plusSeconds(30));

    Page<StockMovement> page = stockMovementRepository.findAllByFilters(
        firstProduct.getId(),
        null,
        StockMovementType.RECEIPT,
        PageRequest.of(0, 10, NEWEST_FIRST)
    );

    assertThat(page.getContent()).extracting(StockMovement::getId)
        .containsExactly(newer.getId(), older.getId());
  }

  @Test
  void returnsEmptyPageForMovementTypeWithoutRecords() {
    Product product = saveProduct("SKU-001");
    saveReceipt(product, 3, CREATED_AT.plusSeconds(10));

    Page<StockMovement> page = stockMovementRepository.findAllByFilters(
        product.getId(),
        null,
        StockMovementType.RESERVATION,
        PageRequest.of(0, 10, NEWEST_FIRST)
    );

    assertThat(page).isEmpty();
  }

  private Product saveProduct(String sku) {
    return productRepository.saveAndFlush(Product.create(sku, "상품", CREATED_AT));
  }

  private StockMovement saveReceipt(Product product, long quantity, Instant occurredAt) {
    Inventory inventory = Inventory.initialize(product, CREATED_AT);
    inventory.receive(quantity, occurredAt);
    return stockMovementRepository.saveAndFlush(
        StockMovement.receipt(inventory, quantity, occurredAt)
    );
  }
}
