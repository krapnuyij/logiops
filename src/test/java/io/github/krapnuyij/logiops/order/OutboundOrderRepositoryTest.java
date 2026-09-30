package io.github.krapnuyij.logiops.order;

import java.time.Instant;
import java.util.List;

import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
class OutboundOrderRepositoryTest {

  private static final Instant CREATED_AT = Instant.parse("2026-09-30T01:00:00Z");

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private OutboundOrderRepository outboundOrderRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void persistsAndLoadsOrderWithItemsAndProducts() {
    Product firstProduct = saveProduct("SKU-001");
    Product secondProduct = saveProduct("SKU-002");
    OutboundOrder saved = saveOrder(firstProduct, secondProduct);
    entityManager.clear();

    OutboundOrder found = outboundOrderRepository.findDetailedById(saved.getId()).orElseThrow();

    assertThat(found.getStatus()).isEqualTo(OutboundOrderStatus.RESERVED);
    assertThat(found.getItems()).hasSize(2)
        .extracting(item -> item.getProduct().getSku())
        .containsExactlyInAnyOrder("SKU-001", "SKU-002");
  }

  @Test
  void enforcesUniqueProductPerOrderAtDatabaseBoundary() {
    Product product = saveProduct("SKU-001");
    OutboundOrder order = saveOrder(product);

    assertThatThrownBy(() -> jdbcTemplate.update(
        "insert into outbound_order_items (outbound_order_id, product_id, quantity) "
            + "values (?, ?, ?)",
        order.getId(),
        product.getId(),
        1
    )).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsNonPositiveItemQuantityAtDatabaseBoundary() {
    Product product = saveProduct("SKU-001");
    OutboundOrder order = saveOrder(product);
    Product secondProduct = saveProduct("SKU-002");

    assertThatThrownBy(() -> jdbcTemplate.update(
        "insert into outbound_order_items (outbound_order_id, product_id, quantity) "
            + "values (?, ?, ?)",
        order.getId(),
        secondProduct.getId(),
        0
    )).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void locksOrderRootForUpdate() {
    OutboundOrder saved = saveOrder(saveProduct("SKU-001"));
    entityManager.flush();
    entityManager.clear();

    OutboundOrder locked = outboundOrderRepository.findByIdForUpdate(saved.getId()).orElseThrow();

    assertThat(entityManager.getLockMode(locked)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    assertThat(locked.getStatus()).isEqualTo(OutboundOrderStatus.RESERVED);
  }

  private Product saveProduct(String sku) {
    return productRepository.saveAndFlush(Product.create(sku, "상품", CREATED_AT));
  }

  private OutboundOrder saveOrder(Product... products) {
    List<OutboundOrder.ItemDraft> items = java.util.Arrays.stream(products)
        .map(product -> new OutboundOrder.ItemDraft(product, 1))
        .toList();
    return outboundOrderRepository.saveAndFlush(
        OutboundOrder.createReserved(items, CREATED_AT)
    );
  }
}
