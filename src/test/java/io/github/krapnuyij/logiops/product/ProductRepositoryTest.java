package io.github.krapnuyij.logiops.product;

import java.time.Instant;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

  private static final Instant CREATED_AT = Instant.parse("2026-09-30T01:00:00.123456Z");

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  void persistsAndLoadsProductUsingMigratedSchema() {
    Product saved = productRepository.saveAndFlush(Product.create("sku-001", "상품", CREATED_AT));
    entityManager.clear();

    Product found = productRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getSku()).isEqualTo("SKU-001");
    assertThat(found.getName()).isEqualTo("상품");
    assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void enforcesUniqueSkuConstraint() {
    productRepository.saveAndFlush(Product.create("sku-001", "첫 상품", CREATED_AT));

    assertThatThrownBy(() -> productRepository.saveAndFlush(
        Product.create("SKU-001", "두 번째 상품", CREATED_AT)
    )).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void pagesProductsInIdAscendingOrder() {
    Product first = productRepository.save(Product.create("SKU-002", "첫 상품", CREATED_AT));
    Product second = productRepository.save(Product.create("SKU-001", "두 번째 상품", CREATED_AT));
    productRepository.flush();

    Page<Product> page = productRepository.findAll(
        PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"))
    );

    assertThat(page.getContent()).extracting(Product::getId)
        .containsExactly(first.getId(), second.getId());
  }
}
