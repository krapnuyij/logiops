package io.github.krapnuyij.logiops.product;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "products",
    uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
)
public class Product {

  private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
  private static final int MAX_NAME_LENGTH = 100;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64, updatable = false)
  private String sku;

  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Product() {
  }

  private Product(String sku, String name, Instant createdAt) {
    this.sku = normalizeSku(sku);
    this.name = normalizeName(name);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt은 필수이다.");
  }

  public static Product create(String sku, String name, Instant createdAt) {
    return new Product(sku, name, createdAt);
  }

  private static String normalizeSku(String sku) {
    if (sku == null) {
      throw new InvalidProductException("sku", "SKU는 필수이다.");
    }

    String strippedSku = sku.strip();
    if (!SKU_PATTERN.matcher(strippedSku).matches()) {
      throw new InvalidProductException(
          "sku",
          "SKU는 영문자, 숫자, 점, 밑줄, 하이픈으로 구성된 1~64자여야 한다."
      );
    }
    return strippedSku.toUpperCase(Locale.ROOT);
  }

  private static String normalizeName(String name) {
    if (name == null) {
      throw new InvalidProductException("name", "상품명은 필수이다.");
    }

    String strippedName = name.strip();
    if (strippedName.isEmpty() || strippedName.length() > MAX_NAME_LENGTH) {
      throw new InvalidProductException("name", "상품명은 1~100자여야 한다.");
    }
    return strippedName;
  }

  public Long getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
