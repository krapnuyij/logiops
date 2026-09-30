package io.github.krapnuyij.logiops.product.dto;

import java.time.Instant;

import io.github.krapnuyij.logiops.product.Product;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    Instant createdAt
) {

  public static ProductResponse from(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getSku(),
        product.getName(),
        product.getCreatedAt()
    );
  }
}
