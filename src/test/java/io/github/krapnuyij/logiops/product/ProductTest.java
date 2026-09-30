package io.github.krapnuyij.logiops.product;

import java.time.Instant;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

  private static final Instant CREATED_AT = Instant.parse("2026-09-30T01:00:00Z");

  @Test
  void createsProductWithNormalizedValues() {
    Product product = Product.create("  sku-001  ", "  테스트 상품  ", CREATED_AT);

    assertThat(product.getSku()).isEqualTo("SKU-001");
    assertThat(product.getName()).isEqualTo("테스트 상품");
    assertThat(product.getCreatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void normalizesSkuIndependentlyOfDefaultLocale() {
    Locale originalLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));

      Product product = Product.create("mini-item", "상품", CREATED_AT);

      assertThat(product.getSku()).isEqualTo("MINI-ITEM");
    }
    finally {
      Locale.setDefault(originalLocale);
    }
  }

  @Test
  void acceptsSkuAndNameBoundaryLengths() {
    Product product = Product.create("A".repeat(64), "가".repeat(100), CREATED_AT);

    assertThat(product.getSku()).hasSize(64);
    assertThat(product.getName()).hasSize(100);
  }

  @Test
  void rejectsInvalidSku() {
    assertThatThrownBy(() -> Product.create("SKU 001", "상품", CREATED_AT))
        .isInstanceOf(InvalidProductException.class)
        .hasMessageContaining("SKU");
    assertThatThrownBy(() -> Product.create("A".repeat(65), "상품", CREATED_AT))
        .isInstanceOf(InvalidProductException.class);
    assertThatThrownBy(() -> Product.create("한글-SKU", "상품", CREATED_AT))
        .isInstanceOf(InvalidProductException.class);
    assertThatThrownBy(() -> Product.create(null, "상품", CREATED_AT))
        .isInstanceOf(InvalidProductException.class);
  }

  @Test
  void rejectsInvalidName() {
    assertThatThrownBy(() -> Product.create("SKU-001", "  ", CREATED_AT))
        .isInstanceOf(InvalidProductException.class)
        .hasMessageContaining("상품명");
    assertThatThrownBy(() -> Product.create("SKU-001", "가".repeat(101), CREATED_AT))
        .isInstanceOf(InvalidProductException.class);
    assertThatThrownBy(() -> Product.create("SKU-001", null, CREATED_AT))
        .isInstanceOf(InvalidProductException.class);
  }
}
